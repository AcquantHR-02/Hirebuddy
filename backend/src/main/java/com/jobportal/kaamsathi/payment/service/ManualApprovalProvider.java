package com.jobportal.kaamsathi.payment.service;

import com.jobportal.kaamsathi.payment.entity.PaymentStatus;
import org.springframework.stereotype.Component;

/**
 * Fake payment (Reference §2: no real money). We cannot verify anything,
 * so every request waits for the admin to look at the transaction ID / screenshot.
 */
@Component
public class ManualApprovalProvider implements PaymentProvider {

    @Override
    public PaymentStatus check(String transactionId, int amountRupees) {
        return PaymentStatus.PENDING_APPROVAL;
    }
}
