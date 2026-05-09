package com.marsemlixo.api.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class AuthGuardConfig {

    private final boolean authEnabled;
    private final Environment environment;

    public AuthGuardConfig(
            @Value("${app.auth.enabled:false}") boolean authEnabled,
            Environment environment) {
        this.authEnabled = authEnabled;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd && !authEnabled) {
            throw new IllegalStateException(
                    "app.auth.enabled=false não é permitido com perfil 'prod'. " +
                    "Configure APP_AUTH_ENABLED=true ou implante com perfil de dev.");
        }
    }
}
