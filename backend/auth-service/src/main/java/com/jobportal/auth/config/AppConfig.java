package com.jobportal.auth.config;

import com.jobportal.auth.token.JwtProperties;
import com.jobportal.shared.error.GlobalExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

/**
 * Shared building blocks (beans) for the whole auth-service.
 * Spring finds this class by component scan (it is under com.jobportal.auth).
 */
@Configuration
// Switch on the shared error handler from shared-lib (other package, so scan won't find it)
@Import(GlobalExceptionHandler.class)
// Turn the `jwt:` part of application.yml into a JwtProperties bean
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {

    // The one clock the whole service uses to know "now" (always UTC)
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // Turns a password into a one-way BCrypt hash, and checks a password against a hash.
    // 12 = strength ("cost"): 2^12 rounds, about 0.2 s per hash. Slow on purpose = hard to brute-force.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
