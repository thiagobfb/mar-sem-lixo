package com.marsemlixo.api.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

@Configuration
public class GoogleJwksConfig {

    @Bean
    @ConditionalOnMissingBean
    public JWKSource<SecurityContext> googleJwkSource(
            @Value("${app.google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwksUri) {
        try {
            return new RemoteJWKSet<>(new URL(jwksUri));
        } catch (Exception e) {
            throw new IllegalStateException("URI do JWKS do Google inválida: " + jwksUri, e);
        }
    }
}
