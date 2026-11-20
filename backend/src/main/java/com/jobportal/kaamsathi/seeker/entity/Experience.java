package com.jobportal.kaamsathi.seeker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

/**
 * One job the seeker had (a row of seeker_experience). @Embeddable = lives inside the profile.
 */
@Embeddable
public record Experience(

        @Column(name = "job_title", nullable = false, length = 100)
        String jobTitle,

        @Column(name = "company_name", nullable = false, length = 150)
        String companyName,

        @Column(name = "start_date", nullable = false)
        LocalDate startDate,

        @Column(name = "end_date")
        LocalDate endDate,         // null = current job

        @Column(length = 2000)
        String description
) {
}
