package com.jobportal.auth.token.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /auth/login (same for all roles). Example:
 * { "email": "ravi@example.com", "password": "secret123" }
 * Only light checks here: is something there, is it not too long.
 * Whether the email/password is RIGHT is decided later by the service (one reply for both cases).
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email is too long")
        String email,

        // no min = 8 here: login must not teach password rules, and old passwords must still work if rules change
        // max = 72 like signup: BCrypt only looks at the first 72 bytes
        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password is too long")
        String password
) {
}
