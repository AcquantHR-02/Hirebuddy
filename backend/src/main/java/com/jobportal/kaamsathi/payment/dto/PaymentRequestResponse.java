package com.jobportal.kaamsathi.payment.dto;

import com.jobportal.kaamsathi.common.dto.NamedRef;
import com.jobportal.kaamsathi.payment.entity.PaymentRequest;
import com.jobportal.kaamsathi.payment.entity.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * One payment request as the API shows it (owner's billing history, admin queue).
 * plan = {id, name} so the screen can print "Pro" without another call.
 */
public record PaymentRequestResponse(
        UUID id,
        UUID companyId,
        NamedRef plan,
        int amountRupees,
        String transactionId,
        UUID screenshotFileId,      // null = no screenshot sent
        PaymentStatus status,
        String rejectionReason,     // only when REJECTED
        Instant createdAt,
        Instant reviewedAt          // null while PENDING_APPROVAL
) {

    public static PaymentRequestResponse from(PaymentRequest request, String planName) {
        return new PaymentRequestResponse(
                request.getId(),
                request.getCompanyId(),
                new NamedRef(request.getPlanId(), planName),
                request.getAmountRupees(),
                request.getTransactionId(),
                request.getScreenshotFileId(),
                request.getStatus(),
                request.getRejectionReason(),
                request.getCreatedAt(),
                request.getReviewedAt()
        );
    }
}
