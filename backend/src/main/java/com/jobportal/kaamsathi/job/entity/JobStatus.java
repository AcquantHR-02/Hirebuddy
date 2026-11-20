package com.jobportal.kaamsathi.job.entity;

/**
 * Life of a job. Stored as text in jobs.status (V8 CHECK allows exactly these).
 */
public enum JobStatus {
    DRAFT,      // not used yet (posting goes straight to ACTIVE)
    ACTIVE,     // visible in search
    CLOSED,     // closed by the company
    EXPIRED,    // 60 days passed (scheduler, Phase 5)
    BLOCKED     // blocked by the admin (moderation, later)
}
