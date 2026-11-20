package com.jobportal.kaamsathi.auth.service;

import com.jobportal.kaamsathi.auth.entity.TokenType;
import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.auth.entity.UserStatus;
import com.jobportal.kaamsathi.auth.entity.VerificationToken;
import com.jobportal.kaamsathi.auth.repository.UserRepository;
import com.jobportal.kaamsathi.auth.repository.VerificationTokenRepository;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.util.SecureTokens;
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
 * Emailed one-time codes:
 * - verify email = 6-digit OTP typed on the register page (10 min, 5 wrong tries, resend after 60 s)
 * - reset password = link (30 min)
 * No email system yet (notification module comes later), so the email is LOGGED to the console.
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final Duration OTP_TTL = Duration.ofMinutes(10);               // short: a 6-digit code is guessable
    private static final int OTP_MAX_ATTEMPTS = 5;                                 // then the code is dead
    private static final Duration OTP_RESEND_GAP = Duration.ofSeconds(60);         // stops email spam
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);     // short: it can change the password
    private static final Duration ADMIN_SETUP_TTL = Duration.ofHours(24);          // new admin sets the first password

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

    // ---- email verification (OTP) ----

    /**
     * New 6-digit code for this user (valid 10 min). Joins the signup transaction.
     * Returns the plain code ONLY so the dev header can show it (see app.dev.expose-tokens).
     */
    @Transactional
    public String sendEmailVerification(User user) {
        Instant now = Instant.now(clock);
        UUID rowId = UUID.randomUUID();
        String otp = SecureTokens.newOtp();
        verificationTokenRepository.save(new VerificationToken(
                rowId, user.getId(), SecureTokens.otpHash(rowId, otp), TokenType.EMAIL_VERIFY, now.plus(OTP_TTL), now));
        // DEV ONLY: stands in for a real email until the notification module exists
        log.info("EMAIL (dev) to {}: your Kaamsathi verification code is {} (valid 10 minutes)", user.getEmail(), otp);
        return otp;
    }

    /**
     * Register page: email + 6-digit code → account ACTIVE.
     * Only the NEWEST code counts. Wrong code → attempts + 1 (5th wrong try kills the code) → 400 INVALID_OTP.
     * noRollbackFor: the attempts counter must be SAVED even though we throw an error.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void verifyEmail(String rawEmail, String otp) {
        Instant now = Instant.now(clock);
        User user = userRepository.findByEmail(normalize(rawEmail))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_OTP));   // unknown email: same answer
        if (user.getStatus() != UserStatus.UNVERIFIED) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "This email is already verified. Please log in");
        }

        VerificationToken code = verificationTokenRepository
                .findFirstByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), TokenType.EMAIL_VERIFY)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_OTP));   // none / used up / expired

        if (!code.getTokenHash().equals(SecureTokens.otpHash(code.getId(), otp))) {
            code.recordWrongAttempt(now, OTP_MAX_ATTEMPTS);
            throw new ApiException(ErrorCode.INVALID_OTP);
        }

        code.markUsed(now);     // single use
        user.activate(now);     // UNVERIFIED → ACTIVE; both saved by dirty checking at commit
    }

    /**
     * "Resend code": a new OTP for a not-yet-verified account. The old code stops counting (only the newest does).
     * Unknown or already verified email → empty (caller still answers 204: no hint whether it exists).
     * Asked again within 60 s → 429.
     */
    @Transactional
    public Optional<String> resendEmailOtp(String rawEmail) {
        Instant now = Instant.now(clock);
        Optional<User> user = userRepository.findByEmail(normalize(rawEmail))
                .filter(u -> u.getStatus() == UserStatus.UNVERIFIED);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        verificationTokenRepository
                .findFirstByUserIdAndTypeOrderByCreatedAtDesc(user.get().getId(), TokenType.EMAIL_VERIFY)
                .filter(last -> now.isBefore(last.getCreatedAt().plus(OTP_RESEND_GAP)))
                .ifPresent(last -> {
                    throw new ApiException(ErrorCode.OTP_RESEND_TOO_SOON);
                });
        return Optional.of(sendEmailVerification(user.get()));
    }

    // ---- new admin: "set your password" link (reuses the reset-password flow) ----

    /**
     * New admin made by the main admin: send a set-password link (valid 24 h, longer than a normal reset:
     * the person may not see the email at once). Opening it uses POST /api/auth/reset-password,
     * which saves the password AND makes the account ACTIVE. Joins the caller's transaction.
     * Returns the plain token only for the dev header.
     */
    @Transactional
    public String sendAdminSetupLink(User user) {
        String token = SecureTokens.newToken();
        Instant now = Instant.now(clock);
        verificationTokenRepository.save(new VerificationToken(
                UUID.randomUUID(), user.getId(), SecureTokens.sha256(token), TokenType.PASSWORD_RESET,
                now.plus(ADMIN_SETUP_TTL), now));
        log.info("EMAIL (dev) to {}: you were added as a Kaamsathi admin. Set your password -> {}/reset-password?token={}",
                user.getEmail(), frontendUrl, token);
        return token;
    }

    // ---- password reset (link) ----

    /**
     * Forgot password: if the email belongs to an account, make a reset link (valid 30 min).
     * Unknown email → nothing happens, and the caller still answers 204 (no hint whether it exists).
     * Returns the plain token only for the dev header; empty when the email is unknown.
     */
    @Transactional
    public Optional<String> sendPasswordReset(String rawEmail) {
        return userRepository.findByEmail(normalize(rawEmail)).map(user -> {
            String token = SecureTokens.newToken();
            Instant now = Instant.now(clock);
            verificationTokenRepository.save(new VerificationToken(
                    UUID.randomUUID(), user.getId(), SecureTokens.sha256(token), TokenType.PASSWORD_RESET,
                    now.plus(PASSWORD_RESET_TTL), now));
            log.info("EMAIL (dev) to {}: reset your password -> {}/reset-password?token={}",
                    user.getEmail(), frontendUrl, token);
            return token;
        });
    }

    /**
     * Reset link + new password: save the new BCrypt hash, use up the token, log out every device.
     * Unknown / used / expired token → 400 INVALID_LINK.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Instant now = Instant.now(clock);
        VerificationToken stored = verificationTokenRepository
                .findByTokenHashAndType(SecureTokens.sha256(token), TokenType.PASSWORD_RESET)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_LINK));
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_LINK));

        stored.markUsed(now);                                            // single use
        user.changePassword(passwordEncoder.encode(newPassword), now);   // also clears lockout
        user.activate(now);                   // the link came by email → email is proven (UNVERIFIED → ACTIVE)
        tokenService.revokeAll(user.getId()); // a thief who had a session is thrown out
    }

    private static String normalize(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }
}
