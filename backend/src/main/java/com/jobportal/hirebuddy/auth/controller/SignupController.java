package com.jobportal.hirebuddy.auth.controller;

import com.jobportal.hirebuddy.auth.dto.SignupRequest;
import com.jobportal.hirebuddy.auth.dto.UserResponse;
import com.jobportal.hirebuddy.auth.service.SignupService;
import com.jobportal.hirebuddy.auth.service.SignupService.SignupResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP only: read JSON, check it (@Valid), call the service, send JSON back.
 * No business rules here.
 */
@RestController
@RequestMapping("/api/auth/signup")
public class SignupController {

    private static final String DEV_VERIFY_TOKEN_HEADER = "X-Dev-Verify-Token";

    private final SignupService signupService;
    private final boolean exposeTokens;

    public SignupController(SignupService signupService,
                            @Value("${app.dev.expose-tokens}") boolean exposeTokens) {
        this.signupService = signupService;
        this.exposeTokens = exposeTokens;
    }

    /** POST /api/auth/signup/seeker → 201 Created + the new user */
    @PostMapping("/seeker")
    public ResponseEntity<UserResponse> signupSeeker(@Valid @RequestBody SignupRequest request) {
        return created(signupService.signupSeeker(request));
    }

    /** POST /api/auth/signup/company → 201 Created + the new COMPANY_OWNER user (company registered later) */
    @PostMapping("/company")
    public ResponseEntity<UserResponse> signupCompanyOwner(@Valid @RequestBody SignupRequest request) {
        return created(signupService.signupCompanyOwner(request));
    }

    // 201 + user JSON; the verify token goes in a header ONLY when the dev setting is on
    private ResponseEntity<UserResponse> created(SignupResult result) {
        ResponseEntity.BodyBuilder reply = ResponseEntity.status(HttpStatus.CREATED);
        if (exposeTokens) {
            reply.header(DEV_VERIFY_TOKEN_HEADER, result.verifyToken());
        }
        return reply.body(result.user());
    }
}
