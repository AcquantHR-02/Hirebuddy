package com.jobportal.kaamsathi.application.dto;

import java.util.List;

/**
 * One of MY applications with its full tracker: Applied → Viewed → Shortlisted → … (oldest first).
 */
public record ApplicationDetailResponse(
        ApplicationResponse application,
        List<TimelineItem> timeline
) {
}
