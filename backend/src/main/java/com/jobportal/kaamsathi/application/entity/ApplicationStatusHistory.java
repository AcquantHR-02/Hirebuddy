package com.jobportal.kaamsathi.application.entity;

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
 * One status change = one row of `application_status_history` (V15). Never changed after saving
 * (no setters, no behaviour methods): an audit trail only grows.
 */
@Entity
@Table(name = "application_status_history")
public class ApplicationStatusHistory {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "application_id", nullable = false, length = 36)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ApplicationStatus fromStatus;     // null for the first row

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ApplicationStatus toStatus;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "changed_by", length = 36)
    private UUID changedBy;                   // null = system (auto-closure)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApplicationStatusHistory() {
    }

    public ApplicationStatusHistory(UUID id, UUID applicationId, ApplicationStatus fromStatus,
                                    ApplicationStatus toStatus, UUID changedBy, Instant now) {
        this.id = id;
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getApplicationId() { return applicationId; }
    public ApplicationStatus getFromStatus() { return fromStatus; }
    public ApplicationStatus getToStatus() { return toStatus; }
    public UUID getChangedBy() { return changedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
