package com.jobportal.auth.token.dto;

/**
 * JSON reply of POST /auth/login and POST /auth/refresh. Example:
 * { "accessToken": "eyJhbGci...", "refreshToken": "oOdaAzvv...", "expiresIn": 900 }
 * expiresIn = seconds until the access token expires (so the app knows when to refresh).
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
