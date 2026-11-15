package com.jobportal.kaamsathi.auth.service;

import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.auth.entity.UserStatus;
import com.jobportal.kaamsathi.auth.repository.UserRepository;
import com.jobportal.kaamsathi.common.security.Role;
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
 * Runs once when the app starts: creates the ADMIN account from .env if it does not exist yet.
 * Nobody can sign up as admin, so this is the only way an admin is made.
 * The admin password is taken as-is from .env (the 8–72 signup rule is for normal users).
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock,
                       @Value("${app.admin.email}") String adminEmail,
                       @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set in .env - no admin account created");
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return;   // already there: never overwrite (password may have been changed since)
        }

        Instant now = Instant.now(clock);
        userRepository.save(new User(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(adminPassword),   // BCrypt, like everyone else
                Role.ADMIN,
                UserStatus.ACTIVE,                       // admin needs no email verification
                null,                                    // no company
                null,                                    // no signup consent (made by the system)
                now));
        log.info("Admin account created: {}", email);   // never log the password
    }
}
