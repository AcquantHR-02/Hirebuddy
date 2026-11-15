package com.jobportal.kaamsathi.auth.entity;

/**
 * State of a recruiter invite. PENDING → ACCEPTED (expired = PENDING + expires_at in the past).
 */
public enum InviteStatus {
    PENDING,
    ACCEPTED
}
