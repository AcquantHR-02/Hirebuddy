package com.jobportal.kaamsathi.payment.service;

import com.jobportal.kaamsathi.payment.entity.PaymentStatus;

/**
 * The "plug" for checking a payment. PaymentService only talks to this interface,
 * so a real gateway (e.g. Razorpay) can be added later without touching subscription logic.
 *
 * Today there is ONE implementation: ManualApprovalProvider (fake payment, admin decides).
 */
public interface PaymentProvider {

    /**
     * Look at the payment the owner says they made and decide what happens next:
     * PENDING_APPROVAL = a human (admin) must check it,
     * APPROVED         = provider already confirmed it (a real gateway could do this),
     * REJECTED         = provider says it is not valid.
     */
    PaymentStatus check(String transactionId, int amountRupees);
}
