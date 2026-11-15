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
 * One skill = one row of the `skills` table (made by V7__create_masterdata.sql).
 * The admin manages the list; jobs and seekers only pick from it.
 */
@Entity
@Table(name = "skills")
public class Skill {

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

    protected Skill() {
    }

    // a NEW skill is always active
    public Skill(UUID id, String name, Instant now) {
        this.id = id;
        this.name = name;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- behaviour (admin) ----

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
