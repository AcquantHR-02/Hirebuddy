package com.jobportal.kaamsathi.job.controller;

import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.job.dto.SavedJobResponse;
import com.jobportal.kaamsathi.job.service.SavedJobService;
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
 * Saved jobs — SEEKER only (others 403).
 */
@RestController
@RequestMapping("/api/jobs")
@PreAuthorize("hasRole('SEEKER')")
public class SavedJobController {

    private final SavedJobService savedJobService;

    public SavedJobController(SavedJobService savedJobService) {
        this.savedJobService = savedJobService;
    }

    /** POST /api/jobs/{id}/save → 204 (also when already saved). Job not live → 404. */
    @PostMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        savedJobService.save(UUID.fromString(jwt.getSubject()), id);
    }

    /** DELETE /api/jobs/{id}/save → 204 (also when it was not saved). */
    @DeleteMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        savedJobService.unsave(UUID.fromString(jwt.getSubject()), id);
    }

    /** GET /api/jobs/saved?page=0&size=20 → my saved jobs, newest first. */
    @GetMapping("/saved")
    public PageResponse<SavedJobResponse> list(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return savedJobService.list(UUID.fromString(jwt.getSubject()), page, size);
    }
}
