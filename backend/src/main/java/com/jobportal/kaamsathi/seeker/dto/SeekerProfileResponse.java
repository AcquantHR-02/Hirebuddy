package com.jobportal.kaamsathi.seeker.dto;

import com.jobportal.kaamsathi.common.dto.NamedRef;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The seeker's profile as shown to the seeker (and later to recruiters of jobs they applied to).
 * Before the first save every field is empty and complete = false.
 */
public record SeekerProfileResponse(
        UUID userId,
        String fullName,
        String phone,
        String headline,
        String about,
        NamedRef city,                    // "Pune, Maharashtra"
        Integer experienceYears,
        Integer expectedSalary,
        List<NamedRef> skills,            // sorted by name
        List<NamedRef> preferredCities,
        List<EducationItem> education,
        List<ExperienceItem> experience,
        UUID resumeFileId,                // Part 3
        boolean complete,                 // ready to apply: name + resume (Part 4 needs it)
        Instant updatedAt
) {
}
