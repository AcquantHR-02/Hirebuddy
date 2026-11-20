package com.jobportal.kaamsathi.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One saved job = one row of `saved_jobs` (V10). The id IS the pair (seeker, job).
 */
@Entity
@Table(name = "saved_jobs")
public class SavedJob {

    @EmbeddedId
    private SavedJobId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavedJob() {
    }

    public SavedJob(SavedJobId id, Instant now) {
        this.id = id;
        this.createdAt = now;
    }

    public SavedJobId getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}
