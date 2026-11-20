package com.jobportal.kaamsathi.application.entity;

import com.jobportal.kaamsathi.seeker.dto.ProfileSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * One application = one row of `applications` (V14). Job / company / seeker / resume are plain ids:
 * those tables belong to other modules.
 */
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "job_id", nullable = false, length = 36)
    private UUID jobId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 36)
    private UUID companyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "seeker_id", nullable = false, length = 36)
    private UUID seekerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "resume_file_id", nullable = false, length = 36)
    private UUID resumeFileId;            // resume version sent (frozen)

    @Convert(converter = ProfileSnapshotConverter.class)
    @Column(name = "profile_snapshot", nullable = false, columnDefinition = "json")
    private ProfileSnapshot profileSnapshot;   // profile copy (frozen)

    @Column(name = "viewed_at")
    private Instant viewedAt;             // tracker "Viewed"

    @Column(name = "first_response_at")
    private Instant firstResponseAt;      // employer score

    @JdbcTypeCode(SqlTypes.TINYINT)       // column is TINYINT (V16); Java Integer is easier to use
    @Column(name = "rating")
    private Integer rating;               // company-only 1–5, null = not rated

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;            // applied at

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Application() {
    }

    // a NEW application always starts as APPLIED
    public Application(UUID id, UUID jobId, UUID companyId, UUID seekerId, UUID resumeFileId,
                       ProfileSnapshot profileSnapshot, Instant now) {
        this.id = id;
        this.jobId = jobId;
        this.companyId = companyId;
        this.seekerId = seekerId;
        this.status = ApplicationStatus.APPLIED;
        this.resumeFileId = resumeFileId;
        this.profileSnapshot = profileSnapshot;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // seeker takes it back. Only while it is still running; the caller turns false into a 409.
    public boolean withdraw(Instant now) {
        if (status.isFinal()) {
            return false;
        }
        status = ApplicationStatus.WITHDRAWN;
        updatedAt = now;
        return true;
    }

    /**
     * Recruiter moves the application. Forward only (skipping is fine); REJECTED from any running stage.
     * false = not allowed (already final, backward, or same stage) → the caller turns it into a 409.
     * (VIEWED on first open is done by an atomic UPDATE in the repository, not here.)
     */
    public boolean moveTo(ApplicationStatus target, Instant now) {
        if (status.isFinal()) {
            return false;
        }
        if (target != ApplicationStatus.REJECTED && !target.isAfter(status)) {
            return false;
        }
        status = target;
        if (viewedAt == null) {
            viewedAt = now;            // acted from the list without opening: they did see it
        }
        if (firstResponseAt == null) {
            firstResponseAt = now;     // the first real reply (employer response score)
        }
        updatedAt = now;
        return true;
    }

    /**
     * Company sets / changes / clears (null) its private rating. Allowed in ANY status.
     * Private: firstResponseAt AND updatedAt are NOT touched (the seeker sees updatedAt; a rating must leave no trace).
     * 1–5 is checked by the DTO (and a DB CHECK).
     */
    public void rate(Integer newRating) {
        rating = newRating;
    }

    public UUID getId() { return id; }
    public UUID getJobId() { return jobId; }
    public UUID getCompanyId() { return companyId; }
    public UUID getSeekerId() { return seekerId; }
    public ApplicationStatus getStatus() { return status; }
    public UUID getResumeFileId() { return resumeFileId; }
    public ProfileSnapshot getProfileSnapshot() { return profileSnapshot; }
    public Instant getViewedAt() { return viewedAt; }
    public Instant getFirstResponseAt() { return firstResponseAt; }
    public Integer getRating() { return rating; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
