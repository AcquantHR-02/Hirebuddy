package com.jobportal.auth.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /auth/signup/seeker. Example:
 * { "email": "ravi@example.com", "password": "secret123", "consent": true }
 * The rules below are checked BEFORE our code runs (when the controller uses @Valid).
 */
public record SeekerSignupRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        @Size(max = 254, message = "Email is too long")
        String email,

        // 72 = BCrypt only uses the first 72 bytes, so longer passwords would be silently cut
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        String password,

        // DPDP Act: the user must tick "I agree" to sign up
        @NotNull(message = "Consent is required")
        @AssertTrue(message = "You must accept the terms to sign up")
        Boolean consent
) {
}
