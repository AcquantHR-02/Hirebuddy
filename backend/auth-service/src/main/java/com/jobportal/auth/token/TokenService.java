package com.jobportal.auth.token;

import com.jobportal.auth.token.dto.TokenResponse;
import com.jobportal.auth.user.User;
import com.jobportal.auth.user.UserRepository;
import com.jobportal.auth.user.UserStatus;
import com.jobportal.shared.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Gives out token pairs and manages refresh tokens:
 * issue (after login), refresh (rotate: old one dies, new pair given), logout (revoke).
 */
@Service
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;   // 256 random bits: impossible to guess

    private final JwtService jwtService;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();   // random for security, not java.util.Random

    public TokenService(JwtService jwtService, JwtProperties properties,
                        RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, Clock clock) {
        this.jwtService = jwtService;
        this.properties = properties;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // New access token + new refresh token. Only the refresh token's HASH goes to the DB.
    @Transactional
    public TokenResponse issue(User user) {
        Instant now = Instant.now(clock);

        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        refreshTokenRepository.save(new RefreshToken(
                UUID.randomUUID(), user.getId(), sha256Hex(refreshToken),
                now.plus(properties.refreshTokenTtl()), now));

        return new TokenResponse(
                jwtService.createAccessToken(user),
                refreshToken,
                properties.accessTokenTtl().toSeconds());
    }

    // Rotation: the sent refresh token is revoked and a brand-new pair is returned
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Instant now = Instant.now(clock);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256Hex(refreshToken))
                .filter(token -> token.isActive(now))          // unknown, revoked or expired -> 401
                .orElseThrow(TokenService::invalidRefreshToken);

        stored.revoke();   // dirty checking: Hibernate writes revoked = TRUE at commit

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(TokenService::invalidRefreshToken);
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(403, "ACCOUNT_SUSPENDED", "This account is suspended");
        }

        return issue(user);
    }

    // Logout: revoke this session's refresh token. Unknown token -> do nothing (same 204 reply, no hints)
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(sha256Hex(refreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);   // 32 bytes -> 64 hex characters (fits CHAR(64))
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(401, "INVALID_REFRESH_TOKEN", "Invalid or expired refresh token");
    }
}
