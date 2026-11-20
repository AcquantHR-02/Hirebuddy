package com.jobportal.kaamsathi.file.entity;

import java.util.Set;

/**
 * What a file is for. Each kind has its own allowed types and max size (checked in FileService).
 */
public enum FileKind {

    PHOTO(2 * 1024 * 1024, Set.of(DetectedType.JPEG, DetectedType.PNG, DetectedType.WEBP)),   // profile picture, ≤ 2 MB
    RESUME(5 * 1024 * 1024, Set.of(DetectedType.PDF, DetectedType.DOCX)),                    // CV, ≤ 5 MB
    LOGO(2 * 1024 * 1024, Set.of(DetectedType.JPEG, DetectedType.PNG, DetectedType.WEBP)),    // company logo (later)
    PAYMENT_PROOF(5 * 1024 * 1024, Set.of(DetectedType.JPEG, DetectedType.PNG, DetectedType.PDF)); // screenshot (Phase 4)

    private final long maxBytes;
    private final Set<DetectedType> allowed;

    FileKind(long maxBytes, Set<DetectedType> allowed) {
        this.maxBytes = maxBytes;
        this.allowed = allowed;
    }

    public long maxBytes() { return maxBytes; }
    public Set<DetectedType> allowed() { return allowed; }
}
