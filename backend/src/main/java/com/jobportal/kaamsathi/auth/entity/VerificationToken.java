package com.jobportal.kaamsathi.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One emailed one-time token (verify email / reset password) = one row of `verification_tokens` (V3).
 * Holds only the SHA-256 hash of the token.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;               // null = not used yet

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VerificationToken() {
    }

    public VerificationToken(UUID id, UUID userId, String tokenHash, TokenType type,
                             Instant expiresAt, Instant now) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.usedAt = null;
        this.createdAt = now;
    }

    // usable = never used AND not expired
    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    // single use: after this, isUsable() is false forever
    public void markUsed(Instant now) {
        if (usedAt == null) {
            usedAt = now;
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public TokenType getType() { return type; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
