package com.jobportal.hirebuddy.auth.service;

import com.jobportal.hirebuddy.auth.dto.LoginRequest;
import com.jobportal.hirebuddy.auth.dto.TokenResponse;
import com.jobportal.hirebuddy.auth.entity.User;
import com.jobportal.hirebuddy.auth.entity.UserStatus;
import com.jobportal.hirebuddy.auth.repository.UserRepository;
import com.jobportal.hirebuddy.common.exception.ApiException;
import com.jobportal.hirebuddy.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Checks email + password and returns an access token + refresh token.
 * Protects against brute force (lockout) and user enumeration (same error, same time).
 */
@Service
public class LoginService {

    private static final int MAX_FAILED_LOGINS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    // a real BCrypt hash of a random text, made once at startup; used when the email is unknown
    private final String dummyHash;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        TokenService tokenService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    // noRollbackFor: a wrong password throws 401, but the +1 failed count must still be SAVED
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now(clock);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // same slow BCrypt work as a real user, so the reply time does not reveal "email not found"
            passwordEncoder.matches(request.password(), dummyHash);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.isLocked(now)) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(now, MAX_FAILED_LOGINS, LOCK_DURATION);   // saved by dirty checking
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        // checked AFTER the password: only the real owner learns the account is suspended
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        // also AFTER the password: only the real owner learns the email is not verified yet
        if (user.getStatus() == UserStatus.UNVERIFIED) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        user.recordSuccessfulLogin(now);
        return tokenService.issueTokens(user.getId(), user.getRole(), user.getCompanyId());
    }
}
