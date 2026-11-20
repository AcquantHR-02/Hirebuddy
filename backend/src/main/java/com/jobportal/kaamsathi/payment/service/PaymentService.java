package com.jobportal.kaamsathi.payment.service;

import com.jobportal.kaamsathi.auth.service.UserService;
import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.util.Paging;
import com.jobportal.kaamsathi.company.service.CompanyService;
import com.jobportal.kaamsathi.file.entity.FileKind;
import com.jobportal.kaamsathi.file.service.FileService;
import com.jobportal.kaamsathi.payment.dto.PaymentRequestResponse;
import com.jobportal.kaamsathi.payment.dto.SubmitPaymentRequest;
import com.jobportal.kaamsathi.payment.entity.PaymentRequest;
import com.jobportal.kaamsathi.payment.entity.PaymentStatus;
import com.jobportal.kaamsathi.payment.entity.Plan;
import com.jobportal.kaamsathi.payment.repository.PaymentRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Owner side of payments: send a (fake) payment for a plan + see billing history.
 * Admin side (approve / reject → subscription) comes in Part 4.3.
 */
@Service
public class PaymentService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PlanService planService;
    private final PaymentProvider paymentProvider;      // interface → today ManualApprovalProvider
    private final CompanyService companyService;
    private final UserService userService;
    private final FileService fileService;
    private final Clock clock;

    public PaymentService(PaymentRequestRepository paymentRequestRepository, PlanService planService,
                          PaymentProvider paymentProvider, CompanyService companyService,
                          UserService userService, FileService fileService, Clock clock) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.planService = planService;
        this.paymentProvider = paymentProvider;
        this.companyService = companyService;
        this.userService = userService;
        this.fileService = fileService;
        this.clock = clock;
    }

    /**
     * Owner says "I paid for this plan". Order = cheap checks first, file upload last
     * (so a rejected request doesn't leave a stray screenshot on disk).
     */
    @Transactional
    public PaymentRequestResponse submit(UUID ownerId, SubmitPaymentRequest request, MultipartFile screenshot) {
        UUID companyId = companyService.requireVerifiedCompany(ownerId);       // 409 no company / not verified
        Plan plan = planService.requireBuyable(request.planId());               // 404 unknown / hidden plan
        if (paymentRequestRepository.existsByCompanyIdAndStatus(companyId, PaymentStatus.PENDING_APPROVAL)) {
            throw new ApiException(ErrorCode.PAYMENT_ALREADY_PENDING);         // friendly check
        }

        UUID screenshotId = null;
        if (screenshot != null && !screenshot.isEmpty()) {                     // optional
            screenshotId = fileService.upload(ownerId, FileKind.PAYMENT_PROOF, screenshot).id(); // 413 / 415
        }

        String transactionId = request.transactionId().trim();
        int amount = plan.getPriceRupees();                                     // COPY the price now
        Instant now = clock.instant();
        PaymentRequest payment = new PaymentRequest(UUID.randomUUID(), companyId, plan.getId(),
                amount, transactionId, screenshotId, ownerId, now);

        // ask the provider (fake one always says PENDING_APPROVAL; a real gateway could decide now)
        PaymentStatus decision = paymentProvider.check(transactionId, amount);
        if (decision == PaymentStatus.REJECTED) {
            payment.reject(null, "Payment could not be verified", now);       // null reviewer = system
        } else if (decision == PaymentStatus.APPROVED) {
            payment.approve(null, now);                                        // 4.3: also start the subscription
        }

        try {
            paymentRequestRepository.saveAndFlush(payment);                    // INSERT now → race fails inside try
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.PAYMENT_ALREADY_PENDING);         // DB UNIQUE pending_company_id
        }
        return PaymentRequestResponse.from(payment, plan.getName());
    }

    // owner's billing history, newest first
    @Transactional(readOnly = true)
    public PageResponse<PaymentRequestResponse> listMine(UUID ownerId, Integer page, Integer size) {
        UUID companyId = userService.getCompanyId(ownerId);
        if (companyId == null) {
            throw new ApiException(ErrorCode.COMPANY_REQUIRED);
        }
        Page<PaymentRequest> requests =
                paymentRequestRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, Paging.of(page, size));
        Map<UUID, String> planNames = planService.namesByIds(
                requests.getContent().stream().map(PaymentRequest::getPlanId).toList());
        return PageResponse.from(requests, r -> PaymentRequestResponse.from(r, planNames.get(r.getPlanId())));
    }
}
