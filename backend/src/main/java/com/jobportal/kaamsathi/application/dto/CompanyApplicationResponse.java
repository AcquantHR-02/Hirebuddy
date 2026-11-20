package com.jobportal.kaamsathi.application.dto;

import com.jobportal.kaamsathi.application.entity.Application;
import com.jobportal.kaamsathi.application.entity.ApplicationStatus;
import com.jobportal.kaamsathi.seeker.dto.ProfileSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One application as the COMPANY sees it: the full frozen profile + where it stands + timeline.
 * The resume is not inside: it is downloaded separately (GET .../{id}/resume).
 */
public record CompanyApplicationResponse(
        UUID id,
        UUID jobId,
        UUID seekerId,
        ApplicationStatus status,
        ProfileSnapshot profile,       // what the seeker SENT (later profile edits don't change it)
        Instant appliedAt,
        Instant viewedAt,
        Instant firstResponseAt,       // null until the first real move (5e)
        Integer rating,                // company-only 1–5, null = not rated
        List<TimelineItem> timeline,
        List<NoteResponse> notes       // company-only, oldest first
) {

    public static CompanyApplicationResponse from(Application a, List<TimelineItem> timeline,
                                                  List<NoteResponse> notes) {
        return new CompanyApplicationResponse(a.getId(), a.getJobId(), a.getSeekerId(), a.getStatus(),
                a.getProfileSnapshot(), a.getCreatedAt(), a.getViewedAt(), a.getFirstResponseAt(), a.getRating(),
                timeline, notes);
    }
}
