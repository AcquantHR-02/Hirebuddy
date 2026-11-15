package com.jobportal.hirebuddy.company.entity;

/**
 * Company review state. PENDING -> VERIFIED (can post jobs) or REJECTED (with a reason).
 */
public enum CompanyStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
