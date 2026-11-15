package com.jobportal.kaamsathi.auth.service;

import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.auth.repository.UserRepository;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Account life: admin suspends / unsuspends a user; a user deletes their own account (DPDP).
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(UserRepository userRepository, TokenService tokenService,
                          PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Admin: block this user. Login + refresh stop; all devices logged out. Admins can't be suspended. */
    @Transactional
    public UserResponse suspend(UUID userId) {
        Instant now = Instant.now(clock);
        User user = findNonAdmin(userId);
        user.suspend(now);
        tokenService.revokeAll(user.getId());
        log.info("User {} suspended", user.getId());
        return UserResponse.from(user);
    }

    /** Admin: lift the block → ACTIVE. */
    @Transactional
    public UserResponse unsuspend(UUID userId) {
        User user = findNonAdmin(userId);
        user.unsuspend(Instant.now(clock));
        log.info("User {} unsuspended", user.getId());
        return UserResponse.from(user);
    }

    /**
     * The logged-in user deletes their own account (hard delete).
     * Password asked again. DB CASCADE removes their refresh / verification tokens and invites.
     * Later modules (profile, applications…) will clean their own data via an event (Phase 5).
     */
    @Transactional
    public void deleteOwnAccount(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getRole() == Role.ADMIN) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);   // never delete the admin
        }
        // companies.owner_id FK would block the DELETE (500). Say it clearly instead.
        // Checked with the user's own company_id, so auth does not need to call the company module.
        if (user.getRole() == Role.COMPANY_OWNER && user.getCompanyId() != null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "You own a company. Transfer ownership before deleting your account");
        }
        userRepository.delete(user);
        log.info("User {} deleted their account", userId);   // id only, no email (privacy)
    }

    private User findNonAdmin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.ADMIN) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);
        }
        return user;
    }
}
