package com.jobportal.kaamsathi.common.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    /**
     * The one clock of the app (UTC). Inject it instead of calling Instant.now(), so tests can fix the time.
     * Ticks in whole MICROseconds = the precision of our DATETIME(6) columns. Otherwise a response right after
     * a save shows nanoseconds (…198049100Z) and a later read shows the DB value (…198049Z): same moment, two values.
     */
    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
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
