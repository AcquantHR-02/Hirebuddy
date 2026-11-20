package com.jobportal.kaamsathi.auth.controller;

import com.jobportal.kaamsathi.auth.dto.CreateAdminRequest;
import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.service.AdminAccountService;
import com.jobportal.kaamsathi.auth.service.AdminAccountService.CreatedAdmin;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin accounts (admin panel). Class rule: must be an ADMIN (others 403).
 * Adding an admin is further limited to the MAIN admin inside the service (403 SUPER_ADMIN_ONLY).
 */
@RestController
@RequestMapping("/api/admin/admins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private static final String DEV_SETUP_TOKEN_HEADER = "X-Dev-Reset-Token";   // the link token = a reset token

    private final AdminAccountService adminAccountService;
    private final boolean exposeTokens;

    public AdminAccountController(AdminAccountService adminAccountService,
                                  @Value("${app.dev.expose-tokens}") boolean exposeTokens) {
        this.adminAccountService = adminAccountService;
        this.exposeTokens = exposeTokens;
    }

    /** POST /api/admin/admins {email} → 201 + new admin (UNVERIFIED until the set-password link is used). */
    @PostMapping
    public ResponseEntity<UserResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateAdminRequest request) {
        CreatedAdmin created = adminAccountService.createAdmin(UUID.fromString(jwt.getSubject()), request.email());
        ResponseEntity.BodyBuilder reply = ResponseEntity.status(HttpStatus.CREATED);
        if (exposeTokens) {
            reply.header(DEV_SETUP_TOKEN_HEADER, created.setupToken());
        }
        return reply.body(created.user());
    }

    /** GET /api/admin/admins → 200 + all admins, oldest (main admin) first. */
    @GetMapping
    public List<UserResponse> list() {
        return adminAccountService.listAdmins();
    }
}
