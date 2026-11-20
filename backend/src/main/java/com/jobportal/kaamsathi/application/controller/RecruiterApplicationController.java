package com.jobportal.kaamsathi.application.controller;

import com.jobportal.kaamsathi.application.dto.AddNoteRequest;
import com.jobportal.kaamsathi.application.dto.ApplicantSummaryResponse;
import com.jobportal.kaamsathi.application.dto.ChangeStatusRequest;
import com.jobportal.kaamsathi.application.dto.CompanyApplicationResponse;
import com.jobportal.kaamsathi.application.dto.NoteResponse;
import com.jobportal.kaamsathi.application.dto.RateRequest;
import com.jobportal.kaamsathi.application.entity.ApplicationStatus;
import com.jobportal.kaamsathi.application.service.ApplicationService;
import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.file.dto.FileContent;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Company side of applications — COMPANY_OWNER or RECRUITER only (others 403).
 * Which company = read from the DB in the service, never from the token.
 */
@RestController
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'RECRUITER')")
public class RecruiterApplicationController {

    private final ApplicationService applicationService;

    public RecruiterApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** GET /api/jobs/{jobId}/applications?status=SHORTLISTED&page=0&size=20 → applicants, newest first. */
    @GetMapping("/api/jobs/{jobId}/applications")
    public PageResponse<ApplicantSummaryResponse> listForJob(@AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable UUID jobId,
                                                             @RequestParam(required = false) ApplicationStatus status,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size) {
        return applicationService.listForJob(UUID.fromString(jwt.getSubject()), jobId, status, page, size);
    }

    /** GET /api/company/applications/{id} → full application. First open = VIEWED. */
    @GetMapping("/api/company/applications/{id}")
    public CompanyApplicationResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return applicationService.getForCompany(UUID.fromString(jwt.getSubject()), id);
    }

    /** POST /api/company/applications/{id}/status {"status":"SHORTLISTED"} → 200 updated application. */
    @PostMapping("/api/company/applications/{id}/status")
    public CompanyApplicationResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                   @Valid @RequestBody ChangeStatusRequest request) {
        return applicationService.changeStatus(UUID.fromString(jwt.getSubject()), id, request.status());
    }

    /** POST /api/company/applications/{id}/notes {"text":"…"} → 201 the new private note. */
    @PostMapping("/api/company/applications/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse addNote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody AddNoteRequest request) {
        return applicationService.addNote(UUID.fromString(jwt.getSubject()), id, request.text());
    }

    /** DELETE /api/company/applications/{id}/notes/{noteId} → 204 (own notes only). */
    @DeleteMapping("/api/company/applications/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID noteId) {
        applicationService.deleteNote(UUID.fromString(jwt.getSubject()), id, noteId);
    }

    /** PUT /api/company/applications/{id}/rating {"rating":4} (null clears) → 200 updated application. */
    @PutMapping("/api/company/applications/{id}/rating")
    public CompanyApplicationResponse rate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody RateRequest request) {
        return applicationService.rate(UUID.fromString(jwt.getSubject()), id, request.rating());
    }

    /** GET /api/company/applications/{id}/resume → the resume version that was sent (download). */
    @GetMapping("/api/company/applications/{id}/resume")
    public ResponseEntity<Resource> resume(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        FileContent resume = applicationService.openResumeForCompany(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resume.contentType()))
                .contentLength(resume.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(resume.originalName(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noStore())     // personal document: no cache copies
                .body(resume.resource());
    }
}
