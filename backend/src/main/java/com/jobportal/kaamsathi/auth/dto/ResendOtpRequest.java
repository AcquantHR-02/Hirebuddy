package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of POST /api/auth/resend-otp ("Resend code" on the register page).
 */
public record ResendOtpRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        String email
) {
}
