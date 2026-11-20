package com.jobportal.kaamsathi.payment.repository;

import com.jobportal.kaamsathi.payment.entity.PaymentRequest;
import com.jobportal.kaamsathi.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * DB access for the `payment_requests` table. Only the payment module uses this.
 */
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {

    // send: friendly check "already one pending?" (the DB UNIQUE rule is the real guard)
    boolean existsByCompanyIdAndStatus(UUID companyId, PaymentStatus status);

    // owner's billing history, newest first, one page at a time
    // SELECT * FROM payment_requests WHERE company_id = ? ORDER BY created_at DESC LIMIT ?, ?
    Page<PaymentRequest> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    // admin approval queue: oldest waiting first (fair: first come, first served)
    List<PaymentRequest> findByStatusOrderByCreatedAtAsc(PaymentStatus status);
}
