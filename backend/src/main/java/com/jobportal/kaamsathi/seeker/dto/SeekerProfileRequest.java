package com.jobportal.kaamsathi.seeker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JSON body of PUT /api/seekers/me — the WHOLE profile (full replace). Lists may be empty, not null.
 * Do the ids exist (city, skills, preferred cities)? Checked in the service.
 */
public record SeekerProfileRequest(

        @NotBlank(message = "fullName is required")
        @Size(max = 100, message = "fullName must be at most 100 characters")
        String fullName,

        // Indian mobile: 10 digits starting 6-9 (no +91, no spaces)
        @Pattern(regexp = "^[6-9][0-9]{9}$", message = "phone must be a 10-digit Indian mobile number")
        String phone,

        @Size(max = 150, message = "headline must be at most 150 characters")
        String headline,

        @Size(max = 2000, message = "about must be at most 2000 characters")
        String about,

        UUID cityId,

        @NotNull(message = "experienceYears is required")
        @PositiveOrZero(message = "experienceYears must be 0 or more")
        @Max(value = 50, message = "experienceYears must be at most 50")
        Integer experienceYears,

        @PositiveOrZero(message = "expectedSalary must be 0 or more")
        Integer expectedSalary,

        @NotNull(message = "skillIds is required (may be empty)")
        @Size(max = 30, message = "skillIds can have at most 30 skills")
        Set<@NotNull UUID> skillIds,

        @NotNull(message = "preferredCityIds is required (may be empty)")
        @Size(max = 5, message = "preferredCityIds can have at most 5 cities")
        Set<@NotNull UUID> preferredCityIds,

        // @Valid: also check every item inside the list
        @NotNull(message = "education is required (may be empty)")
        @Size(max = 10, message = "education can have at most 10 entries")
        List<@Valid @NotNull EducationItem> education,

        @NotNull(message = "experience is required (may be empty)")
        @Size(max = 20, message = "experience can have at most 20 entries")
        List<@Valid @NotNull ExperienceItem> experience
) {
}
