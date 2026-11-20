package com.jobportal.kaamsathi.job.controller;

import com.jobportal.kaamsathi.job.dto.JobRequest;
import com.jobportal.kaamsathi.job.dto.JobResponse;
import com.jobportal.kaamsathi.job.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Job endpoints for the company side (owner + recruiter). HTTP only; rules live in JobService.
 * Class-level @PreAuthorize: every method here needs one of these roles; seeker / admin get 403.
 */
@RestController
@RequestMapping("/api/jobs")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'RECRUITER')")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /** POST /api/jobs → 201 + job (ACTIVE). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JobRequest request) {
        // poster = token's user; the company is looked up from the DB in the service
        return jobService.create(userId(jwt), request);
    }

    /** GET /api/jobs/mine → 200 + all jobs of my company (any status), newest first. */
    @GetMapping("/mine")
    public List<JobResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return jobService.listMine(userId(jwt));
    }

    /** PUT /api/jobs/{id} → 200 + edited job (full replace). */
    @PutMapping("/{id}")
    public JobResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @Valid @RequestBody JobRequest request) {
        return jobService.update(userId(jwt), id, request);
    }

    /** POST /api/jobs/{id}/close → 200 + job (CLOSED). */
    @PostMapping("/{id}/close")
    public JobResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return jobService.close(userId(jwt), id);
    }

    /** POST /api/jobs/{id}/reopen → 200 + job (ACTIVE again). */
    @PostMapping("/{id}/reopen")
    public JobResponse reopen(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return jobService.reopen(userId(jwt), id);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
