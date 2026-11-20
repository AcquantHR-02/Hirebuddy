package com.jobportal.kaamsathi.job.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One job = one row of `jobs` + its rows in `job_skills` (V8__create_jobs.sql).
 */
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 36)
    private UUID companyId;               // plain id: companies belong to the company module

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", length = 36)
    private UUID createdBy;               // user who posted; NULL if that user was deleted

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "category_id", nullable = false, length = 36)
    private UUID categoryId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "city_id", length = 36)
    private UUID cityId;                  // NULL only for REMOTE

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 20)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @Column(name = "experience_min", nullable = false)
    private short experienceMin;

    @Column(name = "experience_max")
    private Short experienceMax;          // null = no upper limit

    @Column(name = "salary_min")
    private Integer salaryMin;            // rupees per year; null = hidden

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // job_skills table: one row (job_id, skill_id) per skill. No entity needed, just a set of ids.
    // @BatchSize: a search page of 20 jobs loads all their skills in ONE query (no N+1).
    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "skill_id", length = 36, nullable = false)
    private Set<UUID> skillIds = new HashSet<>();

    protected Job() {
    }

    // a NEW job goes straight to ACTIVE (no drafts yet) and expires at the given time
    public Job(UUID id, UUID companyId, UUID createdBy, String title, String description,
               UUID categoryId, UUID cityId, WorkMode workMode, JobType jobType,
               short experienceMin, Short experienceMax, Integer salaryMin, Integer salaryMax,
               Set<UUID> skillIds, Instant expiresAt, Instant now) {
        this.id = id;
        this.companyId = companyId;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.cityId = cityId;
        this.workMode = workMode;
        this.jobType = jobType;
        this.experienceMin = experienceMin;
        this.experienceMax = experienceMax;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.skillIds = new HashSet<>(skillIds);
        this.status = JobStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.closedAt = null;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- behaviour: status rules live with the data. Allowed-or-not checks are in JobService. ----

    public boolean isActive() {
        return status == JobStatus.ACTIVE;
    }

    public boolean isClosed() {
        return status == JobStatus.CLOSED;
    }

    // 60 days are over (the scheduler that sets EXPIRED comes in Phase 5; until then we check the time)
    public boolean isPastExpiry(Instant now) {
        return !now.isBefore(expiresAt);
    }

    // visible to the public: ACTIVE and not past expiry
    public boolean isLive(Instant now) {
        return isActive() && !isPastExpiry(now);
    }

    // full replace of the editable fields. Status, company, poster and expiry stay the same.
    public void update(String title, String description, UUID categoryId, UUID cityId,
                       WorkMode workMode, JobType jobType, short experienceMin, Short experienceMax,
                       Integer salaryMin, Integer salaryMax, Set<UUID> skillIds, Instant now) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.cityId = cityId;
        this.workMode = workMode;
        this.jobType = jobType;
        this.experienceMin = experienceMin;
        this.experienceMax = experienceMax;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.skillIds.clear();              // same Set object: Hibernate sees the change and syncs job_skills
        this.skillIds.addAll(skillIds);
        this.updatedAt = now;
    }

    // ACTIVE → CLOSED (hidden from search; slot stays used: free posts are lifetime)
    public void close(Instant now) {
        status = JobStatus.CLOSED;
        closedAt = now;
        updatedAt = now;
    }

    // CLOSED → ACTIVE again, same expiry date (no extra 60 days)
    public void reopen(Instant now) {
        status = JobStatus.ACTIVE;
        closedAt = null;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getCreatedBy() { return createdBy; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getCityId() { return cityId; }
    public WorkMode getWorkMode() { return workMode; }
    public JobType getJobType() { return jobType; }
    public short getExperienceMin() { return experienceMin; }
    public Short getExperienceMax() { return experienceMax; }
    public Integer getSalaryMin() { return salaryMin; }
    public Integer getSalaryMax() { return salaryMax; }
    public JobStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<UUID> getSkillIds() { return Collections.unmodifiableSet(skillIds); }  // read-only view
}
