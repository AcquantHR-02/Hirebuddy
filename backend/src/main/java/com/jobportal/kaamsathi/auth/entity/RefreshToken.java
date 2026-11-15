package com.jobportal.kaamsathi.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One login session on one device = one row of `refresh_tokens` (V2__create_refresh_tokens.sql).
 * Holds only the SHA-256 hash of the token, never the token itself.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    // plain id, not @ManyToOne User: we never need the whole User when checking a token
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;            // null = still usable

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant now) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = null;
        this.createdAt = now;
    }

    // usable = never revoked AND not expired yet
    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    // cancel it (refresh rotation or logout); revoking twice keeps the first time
    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
