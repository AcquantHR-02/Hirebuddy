package com.jobportal.auth.user.dto;

import com.jobportal.auth.user.User;
import com.jobportal.auth.user.UserStatus;
import com.jobportal.shared.security.Role;

import java.util.UUID;

/**
 * Safe view of an account, used by signup (201) and GET /auth/me (200). Example:
 * { "id": "3f2a9c1e-...", "email": "ravi@example.com", "role": "SEEKER", "status": "UNVERIFIED" }
 * Only safe fields: NEVER the password hash, lock info, etc.
 */
public record UserResponse(
        UUID id,
        String email,
        Role role,
        UserStatus status
) {

    // Copy the safe fields out of the entity
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus());
    }
}
