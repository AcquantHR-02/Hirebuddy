package com.jobportal.hirebuddy.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of POST /api/auth/verify-email. The token comes from the emailed link (?token=...).
 */
public record VerifyEmailRequest(

        @NotBlank(message = "token is required")
        String token
) {
}
