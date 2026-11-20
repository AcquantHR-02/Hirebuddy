package com.jobportal.kaamsathi.job.dto;

import com.jobportal.kaamsathi.common.dto.NamedRef;
import com.jobportal.kaamsathi.job.entity.JobType;
import com.jobportal.kaamsathi.job.entity.WorkMode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One job card in the public search results. No description (keeps the list small).
 */
public record JobSummaryResponse(
        UUID id,
        String title,
        NamedRef company,
        NamedRef category,
        NamedRef city,              // null for REMOTE; name like "Pune, Maharashtra"
        WorkMode workMode,
        JobType jobType,
        int experienceMin,
        Integer experienceMax,
        Integer salaryMin,
        Integer salaryMax,
        List<NamedRef> skills,      // sorted by name
        Instant createdAt,
        Instant expiresAt
) {
}
