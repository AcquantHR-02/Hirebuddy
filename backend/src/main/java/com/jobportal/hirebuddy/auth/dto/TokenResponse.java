package com.jobportal.hirebuddy.auth.dto;

/**
 * Reply of login (and later refresh).
 * accessToken  → send as "Authorization: Bearer <accessToken>" (15 min)
 * refreshToken → send to /api/auth/refresh to get a new pair (7 days, single use)
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,    // always "Bearer"
        long expiresIn       // seconds until the ACCESS token stops working (900 = 15 min)
) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
