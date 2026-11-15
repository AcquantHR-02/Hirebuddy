package com.jobportal.hirebuddy.auth.entity;

/**
 * State of a recruiter invite. PENDING → ACCEPTED (expired = PENDING + expires_at in the past).
 */
public enum InviteStatus {
    PENDING,
    ACCEPTED
}
