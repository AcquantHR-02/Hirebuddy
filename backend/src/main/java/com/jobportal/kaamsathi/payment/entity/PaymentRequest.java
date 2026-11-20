package com.jobportal.kaamsathi.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One (fake) payment = one row of `payment_requests` (made by V18__create_payment_requests.sql).
 * The column `pending_company_id` is NOT mapped on purpose: MySQL fills it (generated column).
 * Its UNIQUE rule is our "max 1 pending per company" guard.
 */
@Entity
@Table(name = "payment_requests")
public class PaymentRequest {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 36)
    private UUID companyId;               // plain id: companies belong to the company module

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "plan_id", nullable = false, length = 36)
    private UUID planId;

    @Column(name = "amount_rupees", nullable = false)
    private int amountRupees;             // COPY of the plan price when the request was sent

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "screenshot_file_id", length = 36)
    private UUID screenshotFileId;        // optional proof image

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;       // only when REJECTED

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "requested_by", length = 36)
    private UUID requestedBy;             // owner who sent it (null if that user was deleted)

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "reviewed_by", length = 36)
    private UUID reviewedBy;              // admin who approved / rejected

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentRequest() {
    }

    // a NEW request always waits for the admin
    public PaymentRequest(UUID id, UUID companyId, UUID planId, int amountRupees, String transactionId,
                          UUID screenshotFileId, UUID requestedBy, Instant now) {
        this.id = id;
        this.companyId = companyId;
        this.planId = planId;
        this.amountRupees = amountRupees;
        this.transactionId = transactionId;
        this.screenshotFileId = screenshotFileId;
        this.status = PaymentStatus.PENDING_APPROVAL;
        this.requestedBy = requestedBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- behaviour (admin). "Only from PENDING" is checked in the service (→ clear 409). ----

    public boolean isPending() {
        return status == PaymentStatus.PENDING_APPROVAL;
    }

    public void approve(UUID adminId, Instant now) {
        this.status = PaymentStatus.APPROVED;
        this.reviewedBy = adminId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public void reject(UUID adminId, String reason, Instant now) {
        this.status = PaymentStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedBy = adminId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getPlanId() { return planId; }
    public int getAmountRupees() { return amountRupees; }
    public String getTransactionId() { return transactionId; }
    public UUID getScreenshotFileId() { return screenshotFileId; }
    public PaymentStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getRequestedBy() { return requestedBy; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
