package com.jobportal.shared.error;

import java.time.Instant;

/**
 * The JSON body every service sends back when something goes wrong.
 * Example:
 * { "timestamp": "2026-11-03T10:15:30Z", "status": 404, "error": "NOT_FOUND",
 *   "message": "Job not found", "path": "/core/jobs/123" }
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
