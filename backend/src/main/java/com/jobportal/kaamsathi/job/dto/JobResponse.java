package com.jobportal.kaamsathi.job.dto;

import com.jobportal.kaamsathi.job.entity.Job;
import com.jobportal.kaamsathi.job.entity.JobStatus;
import com.jobportal.kaamsathi.job.entity.JobType;
import com.jobportal.kaamsathi.job.entity.WorkMode;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * What we send back about a job. Only ids for company / category / city / skills for now;
 * names (for search results) come with job search (Part 7).
 */
public record JobResponse(
        UUID id,
        UUID companyId,
        UUID createdBy,
        String title,
        String description,
        UUID categoryId,
        UUID cityId,
        WorkMode workMode,
        JobType jobType,
        int experienceMin,
        Integer experienceMax,
        Integer salaryMin,
        Integer salaryMax,
        Set<UUID> skillIds,
        JobStatus status,
        Instant expiresAt,
        Instant closedAt,         // null unless CLOSED / EXPIRED
        Instant createdAt
) {

    // entity → DTO (called inside the service, inside the transaction: skillIds is loaded lazily)
    public static JobResponse from(Job j) {
        return new JobResponse(
                j.getId(),
                j.getCompanyId(),
                j.getCreatedBy(),
                j.getTitle(),
                j.getDescription(),
                j.getCategoryId(),
                j.getCityId(),
                j.getWorkMode(),
                j.getJobType(),
                j.getExperienceMin(),
                j.getExperienceMax() == null ? null : j.getExperienceMax().intValue(),
                j.getSalaryMin(),
                j.getSalaryMax(),
                Set.copyOf(j.getSkillIds()),
                j.getStatus(),
                j.getExpiresAt(),
                j.getClosedAt(),
                j.getCreatedAt());
    }
}
