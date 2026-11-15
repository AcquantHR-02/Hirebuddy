package com.jobportal.hirebuddy.auth.service;

import com.jobportal.hirebuddy.auth.entity.TokenType;
import com.jobportal.hirebuddy.auth.entity.User;
import com.jobportal.hirebuddy.auth.entity.VerificationToken;
import com.jobportal.hirebuddy.auth.repository.UserRepository;
import com.jobportal.hirebuddy.auth.repository.VerificationTokenRepository;
import com.jobportal.hirebuddy.common.exception.ApiException;
import com.jobportal.hirebuddy.common.exception.ErrorCode;
import com.jobportal.hirebuddy.common.util.SecureTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Emailed one-time links: verify email, reset password.
 * No email system yet (notification module comes later), so the link is LOGGED to the console.
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final Duration EMAIL_VERIFY_TTL = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);   // short: it can change the password

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String frontendUrl;

    public VerificationService(VerificationTokenRepository verificationTokenRepository,
                               UserRepository userRepository, TokenService tokenService,
                               PasswordEncoder passwordEncoder, Clock clock,
                               @Value("${app.frontend-url}") String frontendUrl) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.frontendUrl = frontendUrl;
    }

    /**
     * New verify link for this user (valid 24 h). Joins the signup transaction.
     * Returns the plain token ONLY so the dev header can show it (see app.dev.expose-tokens).
     */
    @Transactional
    public String sendEmailVerification(User user) {
        String token = createToken(user, TokenType.EMAIL_VERIFY, EMAIL_VERIFY_TTL);
        // DEV ONLY: stands in for a real email until the notification module exists
        log.info("EMAIL (dev) to {}: verify your email -> {}/verify-email?token={}",
                user.getEmail(), frontendUrl, token);
        return token;
    }

    /**
     * Forgot password: if the email belongs to an account, make a reset link (valid 30 min).
     * Unknown email → nothing happens, and the caller still answers 204 (no hint whether it exists).
     * Returns the plain token only for the dev header; empty when the email is unknown.
     */
    @Transactional
    public Optional<String> sendPasswordReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(email).map(user -> {
            String token = createToken(user, TokenType.PASSWORD_RESET, PASSWORD_RESET_TTL);
            log.info("EMAIL (dev) to {}: reset your password -> {}/reset-password?token={}",
                    user.getEmail(), frontendUrl, token);
            return token;
        });
    }

    // random token → its hash saved in the DB → plain token returned (goes only into the "email")
    private String createToken(User user, TokenType type, Duration ttl) {
        String token = SecureTokens.newToken();
        Instant now = Instant.now(clock);
        verificationTokenRepository.save(new VerificationToken(
                UUID.randomUUID(), user.getId(), SecureTokens.sha256(token), type, now.plus(ttl), now));
        return token;
    }

    /**
     * Reset link + new password: save the new BCrypt hash, use up the token, log out every device.
     * Unknown / used / expired token → 400 INVALID_LINK.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Instant now = Instant.now(clock);
        VerificationToken stored = findUsable(token, TokenType.PASSWORD_RESET, now);
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_LINK));

        stored.markUsed(now);                                            // single use
        user.changePassword(passwordEncoder.encode(newPassword), now);   // also clears lockout
        user.activate(now);                   // the link came by email → email is proven (UNVERIFIED → ACTIVE)
        tokenService.revokeAll(user.getId()); // a thief who had a session is thrown out
    }

    // hash → row of this type → still usable? else one 400 for unknown / used / expired
    private VerificationToken findUsable(String token, TokenType type, Instant now) {
        return verificationTokenRepository
                .findByTokenHashAndType(SecureTokens.sha256(token), type)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_LINK));
    }

    /**
     * The user clicked the link: mark the token used and make the account ACTIVE.
     * Unknown / used / expired token → 400 INVALID_LINK.
     */
    @Transactional
    public void verifyEmail(String token) {
        Instant now = Instant.now(clock);

        VerificationToken stored = findUsable(token, TokenType.EMAIL_VERIFY, now);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_LINK));

        stored.markUsed(now);   // single use
        user.activate(now);     // both saved by dirty checking at commit
    }
}
