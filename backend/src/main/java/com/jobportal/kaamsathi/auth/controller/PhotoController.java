package com.jobportal.kaamsathi.auth.controller;

import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.auth.service.UserService;
import com.jobportal.kaamsathi.file.dto.FileContent;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Profile picture for EVERY role. All endpoints need a login (SecurityConfig: anyRequest authenticated).
 */
@RestController
@RequestMapping("/api/auth")
public class PhotoController {

    private final UserService userService;

    public PhotoController(UserService userService) {
        this.userService = userService;
    }

    /** Upload or replace my photo: multipart/form-data with a part named "file" (jpeg / png / webp, ≤ 2 MB). */
    // no `consumes` on purpose: a JSON body then gives our clean 400 (MultipartException), not a 500
    @PutMapping("/me/photo")
    public UserResponse changePhoto(@AuthenticationPrincipal Jwt jwt,
                                    @RequestPart("file") MultipartFile file) {
        return userService.changePhoto(UUID.fromString(jwt.getSubject()), file);
    }

    /** Remove my photo → 204 (also when I had none). */
    @DeleteMapping("/me/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePhoto(@AuthenticationPrincipal Jwt jwt) {
        userService.removePhoto(UUID.fromString(jwt.getSubject()));
    }

    /** Show a user's photo (the image bytes, for an <img>). */
    @GetMapping("/users/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable UUID id) {
        FileContent photo = userService.openPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .contentLength(photo.sizeBytes())
                // "inline" = show it in the page, don't download it
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(photo.originalName(), StandardCharsets.UTF_8).build().toString())
                // browser may keep it 5 min, but only for this user (private), never in shared caches
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(photo.resource());
    }
}
