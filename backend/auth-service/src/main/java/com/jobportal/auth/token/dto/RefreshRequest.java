package com.jobportal.auth.token.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /auth/refresh and POST /auth/logout. Example:
 * { "refreshToken": "oOdaAzvvUuqgH9VqF82GG-ZgDEBTDe_Bc2vNF9rqF2w" }
 */
public record RefreshRequest(

        // our refresh tokens are 43 characters; 100 just blocks silly huge input
        @NotBlank(message = "Refresh token is required")
        @Size(max = 100, message = "Refresh token is too long")
        String refreshToken
) {
}
