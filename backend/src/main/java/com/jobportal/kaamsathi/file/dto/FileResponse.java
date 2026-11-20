package com.jobportal.kaamsathi.file.dto;

import com.jobportal.kaamsathi.file.entity.FileKind;
import com.jobportal.kaamsathi.file.entity.StoredFile;

import java.time.Instant;
import java.util.UUID;

/**
 * Facts about an uploaded file (no storage key: where the bytes live is internal).
 */
public record FileResponse(
        UUID id,
        FileKind kind,
        String originalName,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {

    public static FileResponse from(StoredFile f) {
        return new FileResponse(f.getId(), f.getKind(), f.getOriginalName(), f.getContentType(),
                f.getSizeBytes(), f.getCreatedAt());
    }
}
