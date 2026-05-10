package com.marsemlixo.api;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Date;
import java.util.List;

@TestConfiguration
public class TestJwksConfig {

    public static final RSAKey TEST_RSA_KEY;

    static {
        try {
            TEST_RSA_KEY = new RSAKeyGenerator(2048).keyID("test-kid").generate();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Bean
    @Primary
    public JWKSource<SecurityContext> googleJwkSource() {
        return new ImmutableJWKSet<>(new JWKSet(TEST_RSA_KEY.toPublicJWK()));
    }

    public static String criarGoogleIdToken(String googleId, String email, String nome, String clientId) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer("accounts.google.com")
                    .audience(List.of(clientId))
                    .subject(googleId)
                    .claim("email", email)
                    .claim("name", nome)
                    .claim("email_verified", Boolean.TRUE)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-kid").build(),
                    claims);
            jwt.sign(new RSASSASigner(TEST_RSA_KEY));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar Google ID Token de teste", e);
        }
    }
}
