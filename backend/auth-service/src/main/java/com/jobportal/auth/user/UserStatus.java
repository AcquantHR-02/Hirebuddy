package com.jobportal.auth.user;

/**
 * Where an account is in its life.
 * UNVERIFIED (just signed up) → ACTIVE (email verified); an admin can set SUSPENDED.
 * Must match the CHECK rule ck_users_status in V1__create_users.sql.
 */
public enum UserStatus {
    UNVERIFIED,
    ACTIVE,
    SUSPENDED
}
