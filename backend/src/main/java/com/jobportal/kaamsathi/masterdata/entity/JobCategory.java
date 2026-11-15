package com.jobportal.kaamsathi.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One job category = one row of the `job_categories` table (V7). Same pattern as Skill.
 */
@Entity
@Table(name = "job_categories")
public class JobCategory {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active;               // false = hidden from dropdowns (never deleted)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobCategory() {
    }

    public JobCategory(UUID id, String name, Instant now) {
        this.id = id;
        this.name = name;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void rename(String newName, Instant now) {
        this.name = newName;
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        if (!active) {
            active = true;
            updatedAt = now;
        }
    }

    public void deactivate(Instant now) {
        if (active) {
            active = false;
            updatedAt = now;
        }
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
