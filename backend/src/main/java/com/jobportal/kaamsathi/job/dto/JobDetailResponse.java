package com.jobportal.kaamsathi.job.dto;

/**
 * Public job page: the same card as in search + the full description.
 * JSON: {"job": {...card...}, "description": "..."}
 */
public record JobDetailResponse(
        JobSummaryResponse job,
        String description
) {
}
