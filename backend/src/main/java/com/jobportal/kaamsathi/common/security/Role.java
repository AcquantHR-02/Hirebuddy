package com.jobportal.kaamsathi.common.security;

/**
 * Who the user is. Used by many modules (security rules, JWT), so it lives in common/.
 * Saved in the DB as text ("SEEKER"), matching the CHECK in V1__create_users.sql.
 */
public enum Role {
    SEEKER,
    COMPANY_OWNER,
    RECRUITER,
    ADMIN
}
