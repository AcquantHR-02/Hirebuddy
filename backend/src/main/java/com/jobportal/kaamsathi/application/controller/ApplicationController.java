package com.jobportal.kaamsathi.application.controller;

import com.jobportal.kaamsathi.application.dto.ApplicationDetailResponse;
import com.jobportal.kaamsathi.application.dto.ApplicationResponse;
import com.jobportal.kaamsathi.application.service.ApplicationService;
import com.jobportal.kaamsathi.common.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Seeker side of applications — SEEKER only (others 403).
 */
@RestController
@PreAuthorize("hasRole('SEEKER')")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** POST /api/jobs/{jobId}/apply → 201 (no body needed: profile + resume are taken from my account). */
    @PostMapping("/api/jobs/{jobId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID jobId) {
        return applicationService.apply(UUID.fromString(jwt.getSubject()), jobId);
    }

    /** GET /api/applications/mine?page=0&size=20 → my applications, newest first. */
    @GetMapping("/api/applications/mine")
    public PageResponse<ApplicationResponse> listMine(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        return applicationService.listMine(UUID.fromString(jwt.getSubject()), page, size);
    }

    /** GET /api/applications/{id} → my application + its timeline. ("/mine" above wins: exact paths beat {id}.) */
    @GetMapping("/api/applications/{id}")
    public ApplicationDetailResponse getMine(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return applicationService.getMine(UUID.fromString(jwt.getSubject()), id);
    }

    /** POST /api/applications/{id}/withdraw → 200 (status WITHDRAWN). */
    @PostMapping("/api/applications/{id}/withdraw")
    public ApplicationResponse withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return applicationService.withdraw(UUID.fromString(jwt.getSubject()), id);
    }
}
