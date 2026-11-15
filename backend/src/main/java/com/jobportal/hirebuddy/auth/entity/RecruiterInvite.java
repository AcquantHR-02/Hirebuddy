package com.jobportal.hirebuddy.auth.entity;

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
 * One recruiter invite = one row of `recruiter_invites` (V4). Holds only the SHA-256 hash of the token.
 */
@Entity
@Table(name = "recruiter_invites")
public class RecruiterInvite {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 36)
    private UUID companyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "invited_by", nullable = false, length = 36)
    private UUID invitedBy;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InviteStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RecruiterInvite() {
    }

    public RecruiterInvite(UUID id, String email, UUID companyId, UUID invitedBy, String tokenHash,
                           Instant expiresAt, Instant now) {
        this.id = id;
        this.email = email;
        this.companyId = companyId;
        this.invitedBy = invitedBy;
        this.tokenHash = tokenHash;
        this.status = InviteStatus.PENDING;
        this.expiresAt = expiresAt;
        this.acceptedAt = null;
        this.createdAt = now;
    }

    // usable = still PENDING and not expired
    public boolean isUsable(Instant now) {
        return status == InviteStatus.PENDING && now.isBefore(expiresAt);
    }

    // single use: after this, isUsable() is false forever
    public void accept(Instant now) {
        if (status == InviteStatus.PENDING) {
            status = InviteStatus.ACCEPTED;
            acceptedAt = now;
        }
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public UUID getCompanyId() { return companyId; }
    public UUID getInvitedBy() { return invitedBy; }
    public String getTokenHash() { return tokenHash; }
    public InviteStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
