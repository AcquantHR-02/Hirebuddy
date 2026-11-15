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
 * One city = one row of the `cities` table (V7). Like Skill, plus `state`:
 * the PAIR (name, state) is unique, because the same city name exists in different states.
 */
@Entity
@Table(name = "cities")
public class City {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false)
    private boolean active;               // false = hidden from dropdowns (never deleted)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected City() {
    }

    public City(UUID id, String name, String state, Instant now) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // admin fixes the name and/or the state
    public void update(String newName, String newState, Instant now) {
        this.name = newName;
        this.state = newState;
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
    public String getState() { return state; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
