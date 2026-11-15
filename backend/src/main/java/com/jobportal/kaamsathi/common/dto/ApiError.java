package com.jobportal.kaamsathi.common.dto;

import java.time.Instant;

/**
 * The one error JSON shape the whole app returns.
 * Example: {"timestamp":"...","status":404,"code":"NOT_FOUND","message":"...","path":"/api/x"}
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
