package com.jobportal.auth.token;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * The `jwt:` settings from application.yml. Missing value -> app does not start (fail fast).
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        @NotNull Resource privateKey,      // private.pem: signs tokens (only auth-service has it)
        @NotNull Resource publicKey,       // public.pem: checks tokens
        @NotNull Duration accessTokenTtl,  // 15m
        @NotNull Duration refreshTokenTtl  // 7d
) {
}
