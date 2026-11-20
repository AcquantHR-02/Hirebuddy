package com.jobportal.kaamsathi.application.dto;

import com.jobportal.kaamsathi.application.entity.Application;
import com.jobportal.kaamsathi.application.entity.ApplicationStatus;
import com.jobportal.kaamsathi.job.dto.JobSummaryResponse;

import java.time.Instant;
import java.util.UUID;

/**
 * One application as the SEEKER sees it: the job card + where it stands.
 * (The profile snapshot is not repeated here: the seeker knows their own profile.)
 */
public record ApplicationResponse(
        UUID id,
        JobSummaryResponse job,
        ApplicationStatus status,
        Instant viewedAt,          // tracker "Viewed" (null until a recruiter opens it)
        Instant appliedAt,
        Instant updatedAt
) {

    public static ApplicationResponse from(Application a, JobSummaryResponse jobCard) {
        return new ApplicationResponse(a.getId(), jobCard, a.getStatus(), a.getViewedAt(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
