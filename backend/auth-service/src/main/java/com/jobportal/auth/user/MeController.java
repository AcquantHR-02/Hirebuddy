package com.jobportal.auth.user;

import com.jobportal.auth.user.dto.UserResponse;
import com.jobportal.shared.error.ApiException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * GET /auth/me: "who am I?" for the logged-in user (the frontend calls it after login).
 * Spring Security has already checked the token; `jwt` is that checked token.
 */
@RestController
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/auth/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "USER_NOT_FOUND", "User not found"));
        return UserResponse.from(user);
    }
}
