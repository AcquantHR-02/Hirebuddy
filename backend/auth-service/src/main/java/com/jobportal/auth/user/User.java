package com.jobportal.auth.user;

import com.jobportal.shared.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * One account (one row of the `users` table, created by V1__create_users.sql).
 * Hibernate copies data between this object and the row.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)          // store the UUID as text CHAR(36), not binary
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)          // save "SEEKER", not 0
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", length = 36)
    private UUID companyId;               // null for seekers and admins

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;          // null = not locked

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Hibernate needs an empty constructor to build objects from rows. Not for our code.
    protected User() {
    }

    // Our code uses this to make a NEW account. `now` comes from the injected Clock.
    public User(UUID id, String email, String passwordHash, Role role,
                UserStatus status, UUID companyId, Instant now) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.companyId = companyId;
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // locked = a lock time is set and it is still in the future
    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    // wrong password: count it; at maxAttempts lock the account and start counting again from 0
    public void recordFailedLogin(Instant now, int maxAttempts, Duration lockDuration) {
        failedLoginCount++;
        if (failedLoginCount >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginCount = 0;
        }
        updatedAt = now;
    }

    // right password: clear old failures (no DB write if there is nothing to clear)
    public void recordSuccessfulLogin(Instant now) {
        if (failedLoginCount == 0 && lockedUntil == null) {
            return;
        }
        failedLoginCount = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public UUID getCompanyId() { return companyId; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
