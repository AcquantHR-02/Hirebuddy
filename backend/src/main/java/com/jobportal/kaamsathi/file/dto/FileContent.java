package com.jobportal.kaamsathi.file.dto;

import org.springframework.core.io.Resource;

/**
 * A file ready to send back: the bytes + what to tell the browser (type, name, size).
 */
public record FileContent(
        Resource resource,
        String contentType,
        String originalName,
        long sizeBytes
) {
}
