package com.jobportal.kaamsathi.job.dto;

import java.time.Instant;

/**
 * One saved job in the seeker's list: the job card + whether it is still open + when it was saved.
 * A job that was closed / expired after saving stays in the list with live = false (the seeker can remove it).
 */
public record SavedJobResponse(
        JobSummaryResponse job,
        boolean live,
        Instant savedAt
) {
}
