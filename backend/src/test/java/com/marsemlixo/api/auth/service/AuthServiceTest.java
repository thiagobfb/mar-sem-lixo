package com.marsemlixo.api.auth.service;

import com.marsemlixo.api.TestJwksConfig;
import com.marsemlixo.api.auth.domain.RefreshToken;
import com.marsemlixo.api.auth.domain.Voluntario;
import com.marsemlixo.api.auth.domain.VoluntarioRole;
import com.marsemlixo.api.auth.repository.RefreshTokenRepository;
import com.marsemlixo.api.auth.repository.VoluntarioRepository;
import com.marsemlixo.api.exception.TokenInvalidoException;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CLIENT_ID = "test-client-id";
    private static final String JWT_SECRET = "test-secret-key-must-be-at-least-32-bytes!";

    @Mock
    private VoluntarioRepository voluntarioRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Usa chave RSA de teste para validar tokens do Google
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(
                new JWKSet(TestJwksConfig.TEST_RSA_KEY.toPublicJWK()));

        authService = new AuthServiceImpl(
                voluntarioRepository,
                refreshTokenRepository,
                jwkSource,
                CLIENT_ID,
                JWT_SECRET,
                900,
                30
        );
    }

    @Test
    void loginWithGoogle_primeiroLogin_criaVoluntarioComRoleVoluntario() {
        String idToken = TestJwksConfig.criarGoogleIdToken(
                "google-001", "novo@test.com", "Novo User", CLIENT_ID);

        when(voluntarioRepository.findByGoogleId("google-001")).thenReturn(Optional.empty());
        when(voluntarioRepository.save(any(Voluntario.class))).thenAnswer(inv -> {
            Voluntario v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.LoginResult result = authService.loginWithGoogle(idToken);

        assertThat(result.voluntario().role()).isEqualTo(VoluntarioRole.VOLUNTARIO);
        assertThat(result.voluntario().email()).isEqualTo("novo@test.com");
        assertThat(result.voluntario().nome()).isEqualTo("Novo User");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshTokenRaw()).isNotBlank();

        ArgumentCaptor<Voluntario> captor = ArgumentCaptor.forClass(Voluntario.class);
        verify(voluntarioRepository).save(captor.capture());
        assertThat(captor.getValue().getDataCadastro()).isNotNull();
    }

    @Test
    void loginWithGoogle_reautenticacao_atualizaEmailENome() {
        String idToken = TestJwksConfig.criarGoogleIdToken(
                "google-002", "atualizado@test.com", "Nome Atualizado", CLIENT_ID);

        Voluntario existente = voluntarioExistente("google-002", "antigo@test.com", "Nome Antigo");
        when(voluntarioRepository.findByGoogleId("google-002")).thenReturn(Optional.of(existente));
        when(voluntarioRepository.save(any(Voluntario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.LoginResult result = authService.loginWithGoogle(idToken);

        assertThat(result.voluntario().email()).isEqualTo("atualizado@test.com");
        assertThat(result.voluntario().nome()).isEqualTo("Nome Atualizado");
    }

    @Test
    void loginWithGoogle_tokenComAssinaturaIncorreta_lancaTokenInvalidoException() {
        // Token assinado com chave diferente da registrada no JWKSource
        String idTokenInvalido = "eyJhbGciOiJSUzI1NiIsImtpZCI6InRlc3Qta2lkIn0.eyJpc3MiOiJhY2NvdW50cy5nb29nbGUuY29tIn0.assinatura_invalida";

        assertThatThrownBy(() -> authService.loginWithGoogle(idTokenInvalido))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void refresh_tokenValido_retornaNovoAccessTokenENovoRefreshToken() {
        Voluntario voluntario = voluntarioExistente("google-003", "vol@test.com", "Voluntário");
        String rawToken = UUID.randomUUID().toString();

        RefreshToken storedToken = new RefreshToken();
        storedToken.setVoluntario(voluntario);
        storedToken.setTokenHash(hashSha256(rawToken));
        storedToken.setExpiraEm(Instant.now().plusSeconds(86400));
        storedToken.setCriadoEm(Instant.now());

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.RefreshResult result = authService.refresh(rawToken);

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.newRefreshTokenRaw()).isNotBlank();
        assertThat(result.newRefreshTokenRaw()).isNotEqualTo(rawToken);
        verify(refreshTokenRepository).deleteByTokenHash(anyString());
    }

    @Test
    void refresh_tokenNaoEncontrado_lancaTokenInvalidoException() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("token-inexistente"))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void refresh_tokenExpirado_lancaTokenInvalidoException() {
        Voluntario voluntario = voluntarioExistente("google-004", "exp@test.com", "Expirado");
        String rawToken = UUID.randomUUID().toString();

        RefreshToken tokenExpirado = new RefreshToken();
        tokenExpirado.setVoluntario(voluntario);
        tokenExpirado.setTokenHash(hashSha256(rawToken));
        tokenExpirado.setExpiraEm(Instant.now().minusSeconds(1)); // já expirou
        tokenExpirado.setCriadoEm(Instant.now().minusSeconds(86400));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenExpirado));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void logout_tokenValido_deletaDoBank() {
        String rawToken = UUID.randomUUID().toString();

        authService.logout(rawToken);

        verify(refreshTokenRepository).deleteByTokenHash(anyString());
    }

    // Helpers

    private Voluntario voluntarioExistente(String googleId, String email, String nome) {
        Voluntario v = new Voluntario();
        v.setId(1L);
        v.setGoogleId(googleId);
        v.setEmail(email);
        v.setNome(nome);
        v.setRole(VoluntarioRole.VOLUNTARIO);
        v.setDataCadastro(Instant.now());
        return v;
    }

    private String hashSha256(String raw) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
