package com.jobportal.kaamsathi.job.dto;

import java.util.UUID;

/**
 * The little another module needs to know about a job (e.g. application: which company owns it).
 */
public record JobRef(
        UUID id,
        UUID companyId,
        String title
) {
}
