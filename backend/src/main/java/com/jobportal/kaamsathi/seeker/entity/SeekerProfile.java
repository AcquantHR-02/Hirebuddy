package com.jobportal.kaamsathi.seeker.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A seeker's profile = one row of seeker_profiles + its rows in the 4 child tables (V12).
 * The id IS the user's id (one profile per user). Lists are saved as a whole (full replace).
 */
@Entity
@Table(name = "seeker_profiles")
public class SeekerProfile {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", length = 36)
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 15)
    private String phone;

    @Column(length = 150)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String about;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "city_id", length = 36)
    private UUID cityId;

    @Column(name = "experience_years", nullable = false)
    private short experienceYears;

    @Column(name = "expected_salary")
    private Integer expectedSalary;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "resume_file_id", length = 36)
    private UUID resumeFileId;            // set in Part 3 (resume upload)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "seeker_skills", joinColumns = @JoinColumn(name = "user_id"))
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "skill_id", length = 36, nullable = false)
    private Set<UUID> skillIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "seeker_preferred_cities", joinColumns = @JoinColumn(name = "user_id"))
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "city_id", length = 36, nullable = false)
    private Set<UUID> preferredCityIds = new HashSet<>();

    // @OrderColumn: the list order is saved in the "position" column (0, 1, 2…)
    @ElementCollection
    @CollectionTable(name = "seeker_education", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    private List<Education> education = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "seeker_experience", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    private List<Experience> experience = new ArrayList<>();

    protected SeekerProfile() {
    }

    // empty profile for this user; filled by update(...)
    public SeekerProfile(UUID userId, Instant now) {
        this.userId = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // full replace of everything the seeker can edit (resume is changed separately)
    public void update(String fullName, String phone, String headline, String about, UUID cityId,
                       short experienceYears, Integer expectedSalary, Set<UUID> skillIds,
                       Set<UUID> preferredCityIds, List<Education> education, List<Experience> experience,
                       Instant now) {
        this.fullName = fullName;
        this.phone = phone;
        this.headline = headline;
        this.about = about;
        this.cityId = cityId;
        this.experienceYears = experienceYears;
        this.expectedSalary = expectedSalary;
        replace(this.skillIds, skillIds);
        replace(this.preferredCityIds, preferredCityIds);
        replace(this.education, education);
        replace(this.experience, experience);
        this.updatedAt = now;
    }

    // new resume uploaded: point to it. The OLD file is kept (applications keep their resume version).
    public void changeResume(UUID newResumeFileId, Instant now) {
        this.resumeFileId = newResumeFileId;
        this.updatedAt = now;
    }

    // keep the SAME collection object (Hibernate tracks it), only swap its content
    private static <T> void replace(Collection<T> target, Collection<T> source) {
        target.clear();
        target.addAll(source);
    }

    public UUID getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getHeadline() { return headline; }
    public String getAbout() { return about; }
    public UUID getCityId() { return cityId; }
    public short getExperienceYears() { return experienceYears; }
    public Integer getExpectedSalary() { return expectedSalary; }
    public UUID getResumeFileId() { return resumeFileId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<UUID> getSkillIds() { return Collections.unmodifiableSet(skillIds); }
    public Set<UUID> getPreferredCityIds() { return Collections.unmodifiableSet(preferredCityIds); }
    public List<Education> getEducation() { return Collections.unmodifiableList(education); }
    public List<Experience> getExperience() { return Collections.unmodifiableList(experience); }
}
