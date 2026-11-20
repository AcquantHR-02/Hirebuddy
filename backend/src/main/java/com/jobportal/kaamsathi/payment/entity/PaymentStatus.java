package com.jobportal.kaamsathi.payment.entity;

/**
 * Status of a payment request. Saved as text (same values as the CHECK in V18).
 * PENDING_APPROVAL → APPROVED (subscription made) or REJECTED (with a reason; owner may send a new one).
 */
public enum PaymentStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED
}
