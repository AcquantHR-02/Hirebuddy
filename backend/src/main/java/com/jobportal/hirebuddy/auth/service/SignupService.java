package com.jobportal.hirebuddy.auth.service;

import com.jobportal.hirebuddy.auth.dto.SignupRequest;
import com.jobportal.hirebuddy.auth.dto.UserResponse;
import com.jobportal.hirebuddy.auth.entity.User;
import com.jobportal.hirebuddy.auth.entity.UserStatus;
import com.jobportal.hirebuddy.auth.repository.UserRepository;
import com.jobportal.hirebuddy.common.exception.ApiException;
import com.jobportal.hirebuddy.common.exception.ErrorCode;
import com.jobportal.hirebuddy.common.security.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Creates new accounts. Business rules live here, not in the controller.
 */
@Service
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final Clock clock;

    // constructor injection: Spring passes the beans in (no @Autowired needed)
    public SignupService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                         VerificationService verificationService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
        this.clock = clock;
    }

    /**
     * What signup gives back to the controller: the user for the JSON reply, and the plain
     * verify token (used ONLY for the dev header when app.dev.expose-tokens=true).
     */
    public record SignupResult(UserResponse user, String verifyToken) {
    }

    /** Job seeker account. */
    @Transactional
    public SignupResult signupSeeker(SignupRequest request) {
        return signup(request, Role.SEEKER);
    }

    /**
     * Company owner account. The company itself is registered later (company module, Phase 2),
     * so companyId stays null for now.
     */
    @Transactional
    public SignupResult signupCompanyOwner(SignupRequest request) {
        return signup(request, Role.COMPANY_OWNER);
    }

    // same rules for every self-signup; only the role differs
    private SignupResult signup(SignupRequest request, Role role) {
        // " Rahul@Gmail.com " and "rahul@gmail.com" must be the same account
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // fast, friendly check
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Instant now = Instant.now(clock);
        User user = new User(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(request.password()),   // store only the BCrypt hash
                role,
                UserStatus.UNVERIFIED,                        // ACTIVE after email verification
                null,                                         // no company yet (owner registers it in Phase 2)
                now,                                          // consent given now (DPDP)
                now);

        try {
            // saveAndFlush = run the INSERT now, so a duplicate email fails HERE, inside try
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // two signups with the same email at the same moment: the DB UNIQUE key stops the 2nd
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // same transaction: if this fails, the user is not saved either (no account without a link)
        String verifyToken = verificationService.sendEmailVerification(user);

        return new SignupResult(UserResponse.from(user), verifyToken);
    }
}
