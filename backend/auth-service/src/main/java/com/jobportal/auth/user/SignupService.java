package com.jobportal.auth.user;

import com.jobportal.auth.user.dto.SeekerSignupRequest;
import com.jobportal.shared.error.ApiException;
import com.jobportal.shared.security.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * The signup rules ("business logic"). The controller only receives HTTP;
 * this class decides what happens: check email, hash password, save the new account.
 */
@Service
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    // Spring passes in the 3 beans it already has (constructor injection)
    public SignupService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    // All DB work inside runs as ONE transaction: all of it is saved, or none of it
    @Transactional
    public User signupSeeker(SeekerSignupRequest request) {
        // " Ravi@Example.com " -> "ravi@example.com" (one spelling per email)
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Normal case: the email is already used -> 409 Conflict
        if (userRepository.existsByEmail(email)) {
            throw emailTaken();
        }

        // Never store the real password, only its BCrypt hash
        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                UUID.randomUUID(),          // new random id
                email,
                passwordHash,
                Role.SEEKER,                // this endpoint makes job seekers only
                UserStatus.UNVERIFIED,      // becomes ACTIVE after email verification
                null,                       // seekers have no company
                Instant.now(clock)          // "now" from our UTC Clock bean
        );

        try {
            // saveAndFlush = send the INSERT now (not later at commit), so we can catch its error here
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Rare case: two signups with the same email at the same moment.
            // Both passed existsByEmail; the DB UNIQUE rule stopped the second one.
            throw emailTaken();
        }
    }

    private ApiException emailTaken() {
        return new ApiException(409, "EMAIL_TAKEN", "This email is already registered");
    }
}
