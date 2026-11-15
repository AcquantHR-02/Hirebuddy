package com.jobportal.kaamsathi.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    /** The one clock of the app (UTC). Inject it instead of calling Instant.now(), so tests can fix the time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Hashes passwords with BCrypt. Saves them like "$2a$10$..." (60 chars).
     * Code depends on the PasswordEncoder interface, not on BCrypt directly.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
