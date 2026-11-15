package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of POST /api/auth/forgot-password.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email is not valid")
        String email
) {
}
