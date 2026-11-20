package com.jobportal.kaamsathi.application.dto;

import com.jobportal.kaamsathi.application.entity.Application;
import com.jobportal.kaamsathi.application.entity.ApplicationStatus;
import com.jobportal.kaamsathi.common.dto.NamedRef;
import com.jobportal.kaamsathi.seeker.dto.ProfileSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One applicant row in the recruiter's list ("applicant card").
 * Taken from the FROZEN profile copy (what the seeker sent), not the live profile.
 * Short on purpose: phone, about, education, experience come with the detail view (5d).
 */
public record ApplicantSummaryResponse(
        UUID id,                    // application id
        UUID seekerId,
        String fullName,
        String headline,
        NamedRef city,
        Integer experienceYears,
        List<NamedRef> skills,
        ApplicationStatus status,
        Integer rating,             // company-only stars in the list
        Instant appliedAt,
        Instant viewedAt
) {

    public static ApplicantSummaryResponse from(Application a) {
        ProfileSnapshot p = a.getProfileSnapshot();
        return new ApplicantSummaryResponse(a.getId(), a.getSeekerId(), p.fullName(), p.headline(), p.city(),
                p.experienceYears(), p.skills(), a.getStatus(), a.getRating(), a.getCreatedAt(), a.getViewedAt());
    }
}
