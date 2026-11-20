package com.jobportal.kaamsathi.application.dto;

import com.jobportal.kaamsathi.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Recruiter moves an application: {"status": "SHORTLISTED"}.
 * Unknown value → 400 MALFORMED_JSON (Jackson can't map it); missing → 400 VALIDATION_FAILED.
 */
public record ChangeStatusRequest(
        @NotNull(message = "status is required")
        ApplicationStatus status
) {
}
