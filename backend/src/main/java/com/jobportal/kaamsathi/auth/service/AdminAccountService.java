package com.jobportal.kaamsathi.auth.service;

import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.auth.entity.UserStatus;
import com.jobportal.kaamsathi.auth.repository.UserRepository;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.security.Role;
import com.jobportal.kaamsathi.common.util.SecureTokens;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Admin accounts. The FIRST admin comes from .env (AdminSeeder) = the "main admin".
 * Only the main admin may add more admins. New admins get a set-password link, never a password from someone else.
 */
@Service
public class AdminAccountService {

    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final MainAdmin mainAdmin;

    public AdminAccountService(UserRepository userRepository, VerificationService verificationService,
                               PasswordEncoder passwordEncoder, Clock clock, MainAdmin mainAdmin) {
        this.userRepository = userRepository;
        this.verificationService = verificationService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.mainAdmin = mainAdmin;
    }

    // what the controller gets back: the new admin + the plain link token (only for the dev header)
    public record CreatedAdmin(UserResponse user, String setupToken) {
    }

    /**
     * Main admin adds an admin: account ADMIN + UNVERIFIED with a random unusable password,
     * then a set-password link (24 h). Opening it (reset-password) sets the password and makes it ACTIVE.
     */
    @Transactional
    public CreatedAdmin createAdmin(UUID callerId, String rawEmail) {
        requireMainAdmin(callerId);

        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Instant now = clock.instant();
        User admin = new User(
                UUID.randomUUID(),
                email,
                // nobody knows this password: login fails until the person sets their own from the link
                passwordEncoder.encode(SecureTokens.newToken()),
                Role.ADMIN,
                UserStatus.UNVERIFIED,          // becomes ACTIVE when the link is used
                null,                           // no company
                null,                           // no signup consent (made by the main admin)
                now);
        try {
            userRepository.saveAndFlush(admin);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);   // same email added at the same moment
        }

        String token = verificationService.sendAdminSetupLink(admin);
        return new CreatedAdmin(UserResponse.from(admin), token);
    }

    // all admins, oldest first (the main admin is on top). Any admin may see the list.
    @Transactional(readOnly = true)
    public List<UserResponse> listAdmins() {
        return userRepository.findByRoleOrderByCreatedAtAsc(Role.ADMIN).stream()
                .map(UserResponse::from)
                .toList();
    }

    // main admin = see MainAdmin (email from .env, checked on the DB row, not the token)
    private void requireMainAdmin(UUID callerId) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (!mainAdmin.is(caller)) {
            throw new ApiException(ErrorCode.SUPER_ADMIN_ONLY);
        }
    }
}
