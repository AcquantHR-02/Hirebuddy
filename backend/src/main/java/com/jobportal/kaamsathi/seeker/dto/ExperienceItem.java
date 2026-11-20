package com.jobportal.kaamsathi.seeker.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * One past / current job in the profile JSON (request AND response). Dates like "2023-04-01".
 */
public record ExperienceItem(

        @NotBlank(message = "jobTitle is required")
        @Size(max = 100, message = "jobTitle must be at most 100 characters")
        String jobTitle,

        @NotBlank(message = "companyName is required")
        @Size(max = 150, message = "companyName must be at most 150 characters")
        String companyName,

        @NotNull(message = "startDate is required")
        @PastOrPresent(message = "startDate can't be in the future")
        LocalDate startDate,

        @PastOrPresent(message = "endDate can't be in the future")
        LocalDate endDate,             // null = current job

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description
) {

    @AssertTrue(message = "endDate must be on or after startDate")
    @JsonIgnore   // a check, not data: keep "datesValid" out of JSON (API replies + application snapshot)
    public boolean isDatesValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
