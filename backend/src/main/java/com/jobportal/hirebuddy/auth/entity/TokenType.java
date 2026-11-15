package com.jobportal.hirebuddy.auth.entity;

/**
 * What a verification token is for. Saved as text, matching the CHECK in V3__create_verification_tokens.sql.
 */
public enum TokenType {
    EMAIL_VERIFY,
    PASSWORD_RESET
}
