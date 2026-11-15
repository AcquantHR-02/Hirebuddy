package com.jobportal.kaamsathi.auth.controller;

import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin actions on users. The auth module owns users, so these live here
 * (Reference: admin actions live in the module that owns the data). Whole class: ADMIN only.
 */
@RestController
@RequestMapping("/api/auth/users")
@PreAuthorize("hasRole('ADMIN')")   // token role must be ADMIN, else 403 ACCESS_DENIED
public class UserAdminController {

    private final AccountService accountService;

    public UserAdminController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** POST /api/auth/users/{id}/suspend → 200 + user (status SUSPENDED) */
    @PostMapping("/{id}/suspend")
    public UserResponse suspend(@PathVariable UUID id) {
        return accountService.suspend(id);
    }

    /** POST /api/auth/users/{id}/unsuspend → 200 + user (status ACTIVE) */
    @PostMapping("/{id}/unsuspend")
    public UserResponse unsuspend(@PathVariable UUID id) {
        return accountService.unsuspend(id);
    }
}
