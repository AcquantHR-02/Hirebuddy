package com.jobportal.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Database access for refresh_tokens rows. save() comes free from JpaRepository (used at login).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // SELECT * FROM refresh_tokens WHERE token_hash = ?   (refresh + logout: "find this session")
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
