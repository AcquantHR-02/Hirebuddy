package com.jobportal.kaamsathi.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One plan = one row of the `plans` table (made by V17__create_plans.sql).
 * The admin creates plans; companies buy one through a (fake) payment request.
 * No plan bought = free rules (5 posts lifetime, 1 recruiter) — free is NOT a row.
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "price_rupees", nullable = false)
    private int priceRupees;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "job_slots", nullable = false)
    private int jobSlots;                 // max ACTIVE jobs at the same time

    @Column(name = "recruiter_seats", nullable = false)
    private int recruiterSeats;           // max recruiters (active + pending invites)

    @Column(nullable = false)
    private boolean active;               // false = can't be bought anymore (never deleted)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Plan() {
    }

    // a NEW plan is always active
    public Plan(UUID id, String name, int priceRupees, int durationDays,
                int jobSlots, int recruiterSeats, Instant now) {
        this.id = id;
        this.name = name;
        this.priceRupees = priceRupees;
        this.durationDays = durationDays;
        this.jobSlots = jobSlots;
        this.recruiterSeats = recruiterSeats;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- behaviour (admin) ----

    // full replace of the editable fields; old subscriptions keep their own copy of the limits (4.3)
    public void update(String name, int priceRupees, int durationDays,
                       int jobSlots, int recruiterSeats, Instant now) {
        this.name = name;
        this.priceRupees = priceRupees;
        this.durationDays = durationDays;
        this.jobSlots = jobSlots;
        this.recruiterSeats = recruiterSeats;
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
    public int getPriceRupees() { return priceRupees; }
    public int getDurationDays() { return durationDays; }
    public int getJobSlots() { return jobSlots; }
    public int getRecruiterSeats() { return recruiterSeats; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
