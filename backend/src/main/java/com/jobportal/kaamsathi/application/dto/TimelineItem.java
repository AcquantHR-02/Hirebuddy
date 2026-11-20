package com.jobportal.kaamsathi.application.dto;

import com.jobportal.kaamsathi.application.entity.ApplicationStatus;

import java.time.Instant;

/**
 * One step of the seeker's tracker: "VIEWED at 5 Nov". Who did it is NOT shown to the seeker.
 */
public record TimelineItem(
        ApplicationStatus status,
        Instant at
) {
}
