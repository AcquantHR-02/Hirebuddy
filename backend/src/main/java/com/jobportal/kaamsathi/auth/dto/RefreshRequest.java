package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of POST /api/auth/refresh (and later /logout).
 */
public record RefreshRequest(

        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {
}
