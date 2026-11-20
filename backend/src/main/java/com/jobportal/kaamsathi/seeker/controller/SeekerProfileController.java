package com.jobportal.kaamsathi.seeker.controller;

import com.jobportal.kaamsathi.file.dto.FileContent;
import com.jobportal.kaamsathi.seeker.dto.SeekerProfileRequest;
import com.jobportal.kaamsathi.seeker.dto.SeekerProfileResponse;
import com.jobportal.kaamsathi.seeker.service.SeekerProfileService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The seeker's own profile. SEEKER only (others 403).
 */
@RestController
@RequestMapping("/api/seekers/me")
@PreAuthorize("hasRole('SEEKER')")
public class SeekerProfileController {

    private final SeekerProfileService profileService;

    public SeekerProfileController(SeekerProfileService profileService) {
        this.profileService = profileService;
    }

    /** GET /api/seekers/me → 200 + my profile (empty fields before the first save). */
    @GetMapping
    public SeekerProfileResponse getMine(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getMine(UUID.fromString(jwt.getSubject()));
    }

    /** PUT /api/seekers/me → 200 + saved profile (full replace; first call creates it). */
    @PutMapping
    public SeekerProfileResponse saveMine(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody SeekerProfileRequest request) {
        return profileService.saveMine(UUID.fromString(jwt.getSubject()), request);
    }

    /** PUT /api/seekers/me/resume (multipart part "file": pdf / docx ≤ 5 MB) → 200 + profile (complete = true). */
    @PutMapping("/resume")
    public SeekerProfileResponse uploadResume(@AuthenticationPrincipal Jwt jwt,
                                              @RequestPart("file") MultipartFile file) {
        return profileService.uploadResume(UUID.fromString(jwt.getSubject()), file);
    }

    /** GET /api/seekers/me/resume → the file as a download ("attachment"). */
    @GetMapping("/resume")
    public ResponseEntity<Resource> downloadResume(@AuthenticationPrincipal Jwt jwt) {
        FileContent resume = profileService.openResume(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resume.contentType()))
                .contentLength(resume.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(resume.originalName(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noStore())     // personal document: never keep a copy in any cache
                .body(resume.resource());
    }
}
