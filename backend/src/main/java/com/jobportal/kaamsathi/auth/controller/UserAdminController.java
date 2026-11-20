package com.jobportal.kaamsathi.auth.controller;

import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.entity.UserStatus;
import com.jobportal.kaamsathi.auth.service.AccountService;
import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.common.security.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin panel "Users" screen. The auth module owns users, so this lives in auth.
 * Whole class: ADMIN only (else 403). Finer rules (main admin only, never the main admin) are in AccountService.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final AccountService accountService;

    public UserAdminController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** GET /api/admin/users?q=&role=&status=&page=&size= → page of users, newest first. */
    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Role role,
                                           @RequestParam(required = false) UserStatus status,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        return accountService.list(role, status, q, page, size);
    }

    /** POST /api/admin/users/{id}/suspend → 200 + user (SUSPENDED) */
    @PostMapping("/{id}/suspend")
    public UserResponse suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return accountService.suspend(callerId(jwt), id);
    }

    /** POST /api/admin/users/{id}/unsuspend → 200 + user (ACTIVE) */
    @PostMapping("/{id}/unsuspend")
    public UserResponse unsuspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return accountService.unsuspend(callerId(jwt), id);
    }

    /** DELETE /api/admin/users/{id} → 204. Main admin only. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        accountService.deleteUser(callerId(jwt), id);
    }

    private static UUID callerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
