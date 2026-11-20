package com.jobportal.kaamsathi.seeker.dto;

import com.jobportal.kaamsathi.common.dto.NamedRef;

import java.util.List;

/**
 * Frozen copy of a seeker's profile, made when they apply. Names are filled in (not only ids),
 * so it still reads right even if a skill / city is renamed or hidden later.
 * Stored as JSON in applications.profile_snapshot.
 */
public record ProfileSnapshot(
        String fullName,
        String phone,
        String headline,
        String about,
        NamedRef city,
        Integer experienceYears,
        Integer expectedSalary,
        List<NamedRef> skills,
        List<NamedRef> preferredCities,
        List<EducationItem> education,
        List<ExperienceItem> experience
) {
}
