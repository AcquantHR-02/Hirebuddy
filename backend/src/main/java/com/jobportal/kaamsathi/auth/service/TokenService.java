package com.jobportal.kaamsathi.auth.service;

import com.jobportal.kaamsathi.auth.dto.TokenResponse;
import com.jobportal.kaamsathi.auth.entity.RefreshToken;
import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.auth.entity.UserStatus;
import com.jobportal.kaamsathi.auth.repository.RefreshTokenRepository;
import com.jobportal.kaamsathi.auth.repository.UserRepository;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.security.JwtProperties;
import com.jobportal.kaamsathi.common.security.JwtService;
import com.jobportal.kaamsathi.common.security.Role;
import com.jobportal.kaamsathi.common.util.SecureTokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Gives out tokens: access token (JWT) + refresh token (random, stored as SHA-256 hash).
 * Used by login and refresh.
 */
@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;   // same module (auth), so direct use is allowed
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public TokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository,
                        JwtService jwtService, JwtProperties jwtProperties, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    /** New access + refresh token for this user. Joins the caller's transaction (login / refresh). */
    @Transactional
    public TokenResponse issueTokens(UUID userId, Role role, UUID companyId) {
        String accessToken = jwtService.createAccessToken(userId, role, companyId);
        String refreshToken = createRefreshToken(userId);
        return TokenResponse.bearer(accessToken, refreshToken, jwtProperties.accessTokenTtl().toSeconds());
    }

    /**
     * Old refresh token → new access + refresh token (ROTATION: the old one can never be used again).
     * Unknown / revoked / expired → 401. Suspended user → 403.
     */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Instant now = Instant.now(clock);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(SecureTokens.sha256(refreshToken))
                .filter(token -> token.isActive(now))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        stored.revoke(now);   // saved by dirty checking at commit
        return issueTokens(user.getId(), user.getRole(), user.getCompanyId());
    }

    /**
     * Log out this device: revoke its refresh token.
     * Never fails: unknown or already-revoked token → nothing to do (no hint whether it existed).
     */
    @Transactional
    public void logout(String refreshToken) {
        Instant now = Instant.now(clock);
        refreshTokenRepository.findByTokenHash(SecureTokens.sha256(refreshToken))
                .ifPresent(token -> token.revoke(now));   // saved by dirty checking
    }

    /**
     * Log out EVERY device of this user (password reset, suspend).
     * Joins the caller's transaction. The short access token still works until it expires (max 15 min).
     */
    @Transactional
    public void revokeAll(UUID userId) {
        Instant now = Instant.now(clock);
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.revoke(now));   // saved by dirty checking
    }

    // plain token goes to the user ONCE; the DB only keeps its hash
    private String createRefreshToken(UUID userId) {
        String token = SecureTokens.newToken();
        Instant now = Instant.now(clock);
        refreshTokenRepository.save(new RefreshToken(
                UUID.randomUUID(), userId, SecureTokens.sha256(token), now.plus(jwtProperties.refreshTokenTtl()), now));
        return token;
    }
}
