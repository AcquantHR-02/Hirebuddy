package com.jobportal.kaamsathi.seeker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One education entry (a row of seeker_education). @Embeddable = no id of its own, lives inside the profile.
 */
@Embeddable
public record Education(

        @Column(nullable = false, length = 100)
        String degree,

        @Column(nullable = false, length = 150)
        String institute,

        @Column(name = "field_of_study", length = 100)
        String fieldOfStudy,

        @Column(name = "start_year", nullable = false)
        Short startYear,

        @Column(name = "end_year")
        Short endYear              // null = still studying
) {
}
