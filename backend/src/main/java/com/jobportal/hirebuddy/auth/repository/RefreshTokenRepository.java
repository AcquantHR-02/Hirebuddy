package com.jobportal.hirebuddy.auth.repository;

import com.jobportal.hirebuddy.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DB access for the `refresh_tokens` table (auth module only).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // SELECT * FROM refresh_tokens WHERE token_hash = ?   (uses the UNIQUE index → fast)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // all NOT revoked tokens of one user (= all devices still logged in)
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
}
