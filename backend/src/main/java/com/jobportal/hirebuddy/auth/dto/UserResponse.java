package com.jobportal.hirebuddy.auth.dto;

import com.jobportal.hirebuddy.auth.entity.User;
import com.jobportal.hirebuddy.auth.entity.UserStatus;
import com.jobportal.hirebuddy.common.security.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * What we send back about a user. Never contains the password hash or lockout fields.
 */
public record UserResponse(
        UUID id,
        String email,
        Role role,
        UserStatus status,
        Instant createdAt
) {

    // entity → DTO (called inside the service, so the entity never leaves it)
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
