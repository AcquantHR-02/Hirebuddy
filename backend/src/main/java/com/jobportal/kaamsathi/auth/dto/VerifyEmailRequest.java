package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * JSON body of POST /api/auth/verify-email: the email typed at register + the 6-digit code from the email.
 */
public record VerifyEmailRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "otp is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "otp must be 6 digits")
        String otp
) {
}
