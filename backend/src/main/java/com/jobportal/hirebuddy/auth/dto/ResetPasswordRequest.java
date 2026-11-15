package com.jobportal.hirebuddy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/auth/reset-password: the token from the emailed link + the new password.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "token is required")
        String token,

        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 72, message = "newPassword must be 8 to 72 characters")   // same rule as signup
        String newPassword
) {
}
