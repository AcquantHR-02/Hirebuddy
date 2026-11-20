package com.jobportal.kaamsathi.seeker.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One education entry in the profile JSON (request AND response).
 */
public record EducationItem(

        @NotBlank(message = "degree is required")
        @Size(max = 100, message = "degree must be at most 100 characters")
        String degree,

        @NotBlank(message = "institute is required")
        @Size(max = 150, message = "institute must be at most 150 characters")
        String institute,

        @Size(max = 100, message = "fieldOfStudy must be at most 100 characters")
        String fieldOfStudy,

        @NotNull(message = "startYear is required")
        @Min(value = 1950, message = "startYear must be 1950 or later")
        @Max(value = 2100, message = "startYear must be 2100 or earlier")
        Integer startYear,

        @Min(value = 1950, message = "endYear must be 1950 or later")
        @Max(value = 2100, message = "endYear must be 2100 or earlier")
        Integer endYear                // null = still studying
) {

    @AssertTrue(message = "endYear must be >= startYear")
    @JsonIgnore   // a check, not data: keep "yearsValid" out of JSON (API replies + application snapshot)
    public boolean isYearsValid() {
        return startYear == null || endYear == null || endYear >= startYear;
    }
}
