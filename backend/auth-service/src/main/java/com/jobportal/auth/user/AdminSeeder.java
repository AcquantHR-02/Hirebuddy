package com.jobportal.auth.user;

import com.jobportal.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Runs once at startup: makes the ADMIN account from .env (ADMIN_EMAIL / ADMIN_PASSWORD) if it is missing.
 * There is no admin signup endpoint on purpose. An existing admin is never changed.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock,
                       @Value("${admin.email}") String email,
                       @Value("${admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = email.trim().toLowerCase(Locale.ROOT);   // same spelling rule as signup/login
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.save(new User(
                UUID.randomUUID(), email, passwordEncoder.encode(password),
                Role.ADMIN, UserStatus.ACTIVE, null, Instant.now(clock)));
        log.info("Admin account created: {}", email);   // never log the password
    }
}
