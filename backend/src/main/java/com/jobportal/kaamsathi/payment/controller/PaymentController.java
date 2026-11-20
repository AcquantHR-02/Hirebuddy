package com.jobportal.kaamsathi.payment.controller;

import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.payment.dto.PaymentRequestResponse;
import com.jobportal.kaamsathi.payment.dto.SubmitPaymentRequest;
import com.jobportal.kaamsathi.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Owner side of payments. Only the COMPANY_OWNER buys plans (recruiters can't).
 */
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('COMPANY_OWNER')")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/payments, multipart/form-data:
     *   planId=<uuid>, transactionId=<text>, screenshot=<file, optional: jpeg / png / pdf ≤ 5 MB>
     * → 201 + the request (status PENDING_APPROVAL).
     */
    // @ModelAttribute = read the text form fields into the record (JSON @RequestBody can't carry a file)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentRequestResponse submit(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @ModelAttribute SubmitPaymentRequest request,
                                         @RequestPart(value = "screenshot", required = false) MultipartFile screenshot) {
        return paymentService.submit(UUID.fromString(jwt.getSubject()), request, screenshot);
    }

    /** GET /api/payments/mine?page=0&size=20 → 200 billing history, newest first */
    @GetMapping("/mine")
    public PageResponse<PaymentRequestResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        return paymentService.listMine(UUID.fromString(jwt.getSubject()), page, size);
    }
}
