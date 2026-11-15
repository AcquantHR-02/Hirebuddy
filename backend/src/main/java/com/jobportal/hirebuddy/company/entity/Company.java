package com.jobportal.hirebuddy.company.entity;

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
 * One company = one row of the `companies` table (made by V5__create_companies.sql).
 */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "owner_id", nullable = false, length = 36)
    private UUID ownerId;                 // plain id, not @ManyToOne: users belong to the auth module

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 15)
    private String gstin;

    @Column(length = 255)
    private String website;

    @Column(length = 100)
    private String industry;

    @Column(length = 20)
    private String size;

    @Column(length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;       // only when REJECTED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
    }

    // a NEW company always starts PENDING (admin must review it)
    public Company(UUID id, UUID ownerId, String name, String gstin, String website,
                   String industry, String size, String location, Instant now) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.gstin = gstin;
        this.website = website;
        this.industry = industry;
        this.size = size;
        this.location = location;
        this.status = CompanyStatus.PENDING;
        this.rejectionReason = null;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- behaviour: the review rule lives with the data it changes ----

    public boolean isPending() {
        return status == CompanyStatus.PENDING;
    }

    // admin approves → VERIFIED, old reason cleared
    public void verify(Instant now) {
        status = CompanyStatus.VERIFIED;
        rejectionReason = null;
        updatedAt = now;
    }

    // admin rejects → REJECTED + reason the owner will see
    public void reject(String reason, Instant now) {
        status = CompanyStatus.REJECTED;
        rejectionReason = reason;
        updatedAt = now;
    }

    public boolean isVerified() {
        return status == CompanyStatus.VERIFIED;
    }

    // owner fixes the details. A REJECTED company goes back to PENDING (resubmit) and the old reason is cleared.
    public void updateDetails(String name, String gstin, String website, String industry,
                              String size, String location, Instant now) {
        this.name = name;
        this.gstin = gstin;
        this.website = website;
        this.industry = industry;
        this.size = size;
        this.location = location;
        if (status == CompanyStatus.REJECTED) {
            status = CompanyStatus.PENDING;
            rejectionReason = null;
        }
        updatedAt = now;
    }

    // ownership transfer: the company now belongs to another user
    public void changeOwner(UUID newOwnerId, Instant now) {
        this.ownerId = newOwnerId;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getGstin() { return gstin; }
    public String getWebsite() { return website; }
    public String getIndustry() { return industry; }
    public String getSize() { return size; }
    public String getLocation() { return location; }
    public CompanyStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
