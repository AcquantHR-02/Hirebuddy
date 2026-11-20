package com.jobportal.kaamsathi.job.dto;

import com.jobportal.kaamsathi.job.entity.JobType;
import com.jobportal.kaamsathi.job.entity.WorkMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * JSON body for posting (and later editing) a job.
 * Field rules = annotations. Rules between TWO fields = @AssertTrue methods at the bottom.
 * Whether the ids really exist (category, city, skills) is checked in the service.
 */
public record JobRequest(

        @NotBlank(message = "title is required")
        @Size(max = 150, message = "title must be at most 150 characters")
        String title,

        @NotBlank(message = "description is required")
        @Size(max = 10000, message = "description must be at most 10000 characters")
        String description,

        @NotNull(message = "categoryId is required")
        UUID categoryId,

        UUID cityId,                                  // required unless REMOTE (see isCityGiven)

        @NotNull(message = "workMode is required (REMOTE, HYBRID, ONSITE)")
        WorkMode workMode,

        @NotNull(message = "jobType is required (FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT)")
        JobType jobType,

        @NotNull(message = "experienceMin is required")
        @PositiveOrZero(message = "experienceMin must be 0 or more")
        @Max(value = 50, message = "experienceMin must be at most 50")
        Integer experienceMin,

        @PositiveOrZero(message = "experienceMax must be 0 or more")
        @Max(value = 50, message = "experienceMax must be at most 50")
        Integer experienceMax,                        // null = no upper limit

        @PositiveOrZero(message = "salaryMin must be 0 or more")
        Integer salaryMin,                            // rupees per year; null = hidden

        @PositiveOrZero(message = "salaryMax must be 0 or more")
        Integer salaryMax,

        @NotEmpty(message = "skillIds must have at least 1 skill")
        @Size(max = 20, message = "skillIds can have at most 20 skills")
        Set<@NotNull(message = "skillIds must not contain null") UUID> skillIds
) {

    // ---- rules between two fields (error field name = method name without "is") ----

    @AssertTrue(message = "cityId is required unless workMode is REMOTE")
    public boolean isCityGiven() {
        return workMode == WorkMode.REMOTE || cityId != null;
    }

    @AssertTrue(message = "experienceMax must be >= experienceMin")
    public boolean isExperienceRangeValid() {
        return experienceMin == null || experienceMax == null || experienceMax >= experienceMin;
    }

    @AssertTrue(message = "salaryMax must be >= salaryMin")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMax >= salaryMin;
    }
}
