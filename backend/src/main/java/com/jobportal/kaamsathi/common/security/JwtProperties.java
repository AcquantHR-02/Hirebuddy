package com.jobportal.kaamsathi.common.security;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * The `app.jwt:` settings from application.yml, as one typed object.
 * A missing value → the app does not start (fail fast), thanks to @Validated + @NotNull.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotNull Resource privateKey,      // private.pem: SIGNS tokens (secret, never share)
        @NotNull Resource publicKey,       // public.pem: CHECKS tokens
        @NotNull Duration accessTokenTtl,  // "15m" in yml → Duration of 15 minutes
        @NotNull Duration refreshTokenTtl  // "7d"  → 7 days
) {
}
