package com.jobportal.kaamsathi.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of the admin's reject call. The owner sees this reason, so it is required.
 */
public record RejectCompanyRequest(

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason
) {
}
