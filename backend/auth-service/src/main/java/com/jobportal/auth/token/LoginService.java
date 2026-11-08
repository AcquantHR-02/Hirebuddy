package com.jobportal.auth.token;

import com.jobportal.auth.token.dto.LoginRequest;
import com.jobportal.auth.token.dto.TokenResponse;
import com.jobportal.auth.user.User;
import com.jobportal.auth.user.UserRepository;
import com.jobportal.auth.user.UserStatus;
import com.jobportal.shared.error.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * The login rules: check email + password, lock after 5 wrong tries, then give a token pair.
 */
@Service
public class LoginService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    // A real BCrypt hash of a throw-away text. Used when the email is not found,
    // so that case also spends ~300 ms on BCrypt (no timing hint for attackers).
    private final String dummyHash;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        TokenService tokenService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-only-for-timing");
    }

    // noRollbackFor: a wrong password throws ApiException, but the +1 fail count must STILL be saved
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse login(LoginRequest request) {
        // same normalizing as signup, so " Ravi@Example.com " finds "ravi@example.com"
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Optional<User> found = userRepository.findByEmail(email);

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);   // result ignored: only to spend the same time
            throw invalidCredentials();
        }

        User user = found.get();
        Instant now = Instant.now(clock);

        // locked accounts are refused BEFORE the password check, so guessing can't continue
        if (user.isLocked(now)) {
            throw new ApiException(429, "ACCOUNT_LOCKED", "Too many failed logins. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(now, MAX_FAILED_ATTEMPTS, LOCK_DURATION);
            throw invalidCredentials();
        }

        // checked only AFTER the password is right, so strangers learn nothing about the account
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(403, "ACCOUNT_SUSPENDED", "This account is suspended");
        }

        // Decision A (2026-11-07): UNVERIFIED may log in until email verification exists
        user.recordSuccessfulLogin(now);
        return tokenService.issue(user);
    }

    // ONE reply for "no such email" and "wrong password"
    private ApiException invalidCredentials() {
        return new ApiException(401, "INVALID_CREDENTIALS", "Invalid email or password");
    }
}
