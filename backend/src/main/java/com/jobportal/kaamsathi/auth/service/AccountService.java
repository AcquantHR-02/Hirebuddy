package com.jobportal.kaamsathi.auth.service;

import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.auth.entity.UserStatus;
import com.jobportal.kaamsathi.auth.repository.UserRepository;
import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.security.Role;
import com.jobportal.kaamsathi.common.util.Paging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Account life: admins manage users (list, suspend, unsuspend, delete); a user deletes their own account (DPDP).
 *
 * Who may act on whom:
 *   target = seeker / owner / recruiter → ANY admin may suspend / unsuspend; only the MAIN admin may delete
 *   target = another admin              → only the MAIN admin
 *   target = the main admin, or yourself → nobody (409)
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final MainAdmin mainAdmin;
    private final Clock clock;

    public AccountService(UserRepository userRepository, TokenService tokenService,
                          PasswordEncoder passwordEncoder, MainAdmin mainAdmin, Clock clock) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.mainAdmin = mainAdmin;
        this.clock = clock;
    }

    /** Admin "Users" screen: filter by role / status / part of the email, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Role role, UserStatus status, String q, Integer page, Integer size) {
        String text = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);
        return PageResponse.from(userRepository.search(role, status, text, Paging.of(page, size)), UserResponse::from);
    }

    /** Block a user: login + refresh stop, all devices logged out. */
    @Transactional
    public UserResponse suspend(UUID callerId, UUID userId) {
        User target = findTargetFor(callerId, userId);
        target.suspend(clock.instant());
        tokenService.revokeAll(target.getId());
        log.info("User {} suspended by {}", target.getId(), callerId);
        return UserResponse.from(target);
    }

    /** Lift the block → ACTIVE. Same rules as suspend. */
    @Transactional
    public UserResponse unsuspend(UUID callerId, UUID userId) {
        User target = findTargetFor(callerId, userId);
        target.unsuspend(clock.instant());
        log.info("User {} unsuspended by {}", target.getId(), callerId);
        return UserResponse.from(target);
    }

    /**
     * MAIN admin deletes an account (hard delete). DB CASCADE removes tokens, invites, saved jobs;
     * jobs they posted stay (created_by → NULL; jobs belong to the company).
     */
    @Transactional
    public void deleteUser(UUID callerId, UUID userId) {
        User caller = findCaller(callerId);
        if (!mainAdmin.is(caller)) {
            throw new ApiException(ErrorCode.SUPER_ADMIN_ONLY);
        }
        User target = findTargetFor(callerId, userId);
        requireNoCompany(target, "This user owns a company. The company must be transferred first");
        userRepository.delete(target);
        log.info("User {} deleted by {}", userId, callerId);
    }

    /**
     * The logged-in user deletes their own account (hard delete). Password asked again.
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
            // an admin is removed by the main admin, not by themselves
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);
        }
        requireNoCompany(user, "You own a company. Transfer ownership before deleting your account");
        userRepository.delete(user);
        log.info("User {} deleted their account", userId);   // id only, no email (privacy)
    }

    // ---- helpers ----

    // target exists (404) + the caller is allowed to act on it (403 / 409), checked in this order:
    // admin target needs the main admin (403) → never the main admin / never yourself (409)
    private User findTargetFor(UUID callerId, UUID targetId) {
        User caller = findCaller(callerId);
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (target.getRole() == Role.ADMIN && !mainAdmin.is(caller)) {
            throw new ApiException(ErrorCode.SUPER_ADMIN_ONLY);
        }
        if (mainAdmin.is(target)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "The main admin can't be changed");
        }
        if (target.getId().equals(caller.getId())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "You can't do this to your own account");
        }
        return target;
    }

    private User findCaller(UUID callerId) {
        return userRepository.findById(callerId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
    }

    // companies.owner_id FK would block the DELETE (500). Say it clearly instead.
    // Checked with the user's own company_id, so auth does not need to call the company module.
    private static void requireNoCompany(User user, String message) {
        if (user.getRole() == Role.COMPANY_OWNER && user.getCompanyId() != null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, message);
        }
    }
}
