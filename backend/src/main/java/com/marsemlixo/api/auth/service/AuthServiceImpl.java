package com.marsemlixo.api.auth.service;

import com.marsemlixo.api.auth.controller.dto.VoluntarioInfo;
import com.marsemlixo.api.auth.domain.RefreshToken;
import com.marsemlixo.api.auth.domain.Voluntario;
import com.marsemlixo.api.auth.domain.VoluntarioRole;
import com.marsemlixo.api.auth.repository.RefreshTokenRepository;
import com.marsemlixo.api.auth.repository.VoluntarioRepository;
import com.marsemlixo.api.exception.TokenInvalidoException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final VoluntarioRepository voluntarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JWKSource<SecurityContext> googleJwkSource;
    private final String googleClientId;
    private final String jwtSecret;
    private final int jwtExpirySeconds;
    private final int refreshExpiryDays;

    public AuthServiceImpl(
            VoluntarioRepository voluntarioRepository,
            RefreshTokenRepository refreshTokenRepository,
            JWKSource<SecurityContext> googleJwkSource,
            @Value("${app.google.client-id}") String googleClientId,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiry-seconds:900}") int jwtExpirySeconds,
            @Value("${app.refresh.expiry-days:30}") int refreshExpiryDays) {
        this.voluntarioRepository = voluntarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.googleJwkSource = googleJwkSource;
        this.googleClientId = googleClientId;
        this.jwtSecret = jwtSecret;
        this.jwtExpirySeconds = jwtExpirySeconds;
        this.refreshExpiryDays = refreshExpiryDays;
    }

    @Override
    @Transactional
    public LoginResult loginWithGoogle(String idToken) {
        JWTClaimsSet claims = validarGoogleIdToken(idToken);

        String googleId = claims.getSubject();
        String email = (String) claims.getClaim("email");
        String nome = (String) claims.getClaim("name");

        // Busca voluntário existente ou cria novo
        Voluntario voluntario = voluntarioRepository.findByGoogleId(googleId)
                .map(v -> {
                    // Atualiza email e nome em reautenticações
                    v.setEmail(email);
                    v.setNome(nome);
                    return voluntarioRepository.save(v);
                })
                .orElseGet(() -> {
                    Voluntario novo = new Voluntario();
                    novo.setGoogleId(googleId);
                    novo.setEmail(email);
                    novo.setNome(nome);
                    novo.setRole(VoluntarioRole.VOLUNTARIO);
                    novo.setDataCadastro(Instant.now());
                    return voluntarioRepository.save(novo);
                });

        String accessToken = gerarAccessToken(voluntario);
        String rawRefreshToken = gerarEPersistirRefreshToken(voluntario);

        VoluntarioInfo info = new VoluntarioInfo(
                voluntario.getId(), voluntario.getNome(), voluntario.getEmail(), voluntario.getRole());
        return new LoginResult(accessToken, jwtExpirySeconds, rawRefreshToken, info);
    }

    @Override
    @Transactional
    public RefreshResult refresh(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new TokenInvalidoException("Refresh token ausente");
        }

        String hash = hash(refreshTokenRaw);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new TokenInvalidoException("Refresh token não encontrado"));

        if (storedToken.getExpiraEm().isBefore(Instant.now())) {
            // Remove token expirado
            refreshTokenRepository.deleteByTokenHash(hash);
            throw new TokenInvalidoException("Refresh token expirado");
        }

        Voluntario voluntario = storedToken.getVoluntario();

        // Rotação: remove token antigo
        refreshTokenRepository.deleteByTokenHash(hash);

        // Emite novo access token e novo refresh token
        String novoAccessToken = gerarAccessToken(voluntario);
        String novoRawRefreshToken = gerarEPersistirRefreshToken(voluntario);

        return new RefreshResult(novoAccessToken, jwtExpirySeconds, novoRawRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenRaw) {
        // Logout idempotente: ignora token ausente ou em branco
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            return;
        }
        String hash = hash(refreshTokenRaw);
        refreshTokenRepository.deleteByTokenHash(hash);
    }

    private String gerarAccessToken(Voluntario voluntario) {
        try {
            JWSSigner signer = new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8));
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(voluntario.getId().toString())
                    .claim("email", voluntario.getEmail())
                    .claim("role", voluntario.getRole().name())
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(jwtExpirySeconds)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Falha ao gerar JWT", e);
        }
    }

    private String gerarEPersistirRefreshToken(Voluntario voluntario) {
        String raw = UUID.randomUUID().toString();
        String tokenHash = hash(raw);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setVoluntario(voluntario);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiraEm(Instant.now().plusSeconds((long) refreshExpiryDays * 86400));
        refreshToken.setCriadoEm(Instant.now());
        refreshTokenRepository.save(refreshToken);

        return raw;
    }

    private JWTClaimsSet validarGoogleIdToken(String rawToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(rawToken);
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                    signedJWT.getHeader().getAlgorithm(), googleJwkSource);
            DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(keySelector);
            // Validação manual dos claims abaixo
            processor.setJWTClaimsSetVerifier((claims, ctx) -> {});
            JWTClaimsSet claims = processor.process(signedJWT, null);

            String iss = claims.getIssuer();
            if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
                throw new TokenInvalidoException("Issuer inválido");
            }
            if (!claims.getAudience().contains(googleClientId)) {
                throw new TokenInvalidoException("Audience inválido");
            }
            if (claims.getExpirationTime().before(new Date())) {
                throw new TokenInvalidoException("Token expirado");
            }
            Object emailVerified = claims.getClaim("email_verified");
            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new TokenInvalidoException("Email não verificado");
            }
            return claims;
        } catch (TokenInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenInvalidoException("ID Token inválido: " + e.getMessage());
        }
    }

    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
