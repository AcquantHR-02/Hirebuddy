package com.jobportal.hirebuddy.auth.entity;

/**
 * Account state. UNVERIFIED -> ACTIVE (email verified); SUSPENDED by admin.
 * Only the auth module uses it, so it sits next to the User entity.
 */
public enum UserStatus {
    UNVERIFIED,
    ACTIVE,
    SUSPENDED
}
