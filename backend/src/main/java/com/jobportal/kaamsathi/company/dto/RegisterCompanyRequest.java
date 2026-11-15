package com.jobportal.kaamsathi.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/companies (register) and PUT /api/companies/me (edit / resubmit) — same fields, same rules.
 * Only name is required. @Pattern and @Size skip null, so optional fields may be left out.
 */
public record RegisterCompanyRequest(

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        // GSTIN: 2-digit state code + 10-char PAN + entity no. + 'Z' + check char, e.g. 27AAPFU0939F1ZV
        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
                message = "gstin must be a valid 15-character GSTIN (uppercase)")
        String gstin,

        @Size(max = 255, message = "website must be at most 255 characters")
        @Pattern(regexp = "^https?://\\S+$", message = "website must start with http:// or https://")
        String website,

        @Size(max = 100, message = "industry must be at most 100 characters")
        String industry,

        @Size(max = 20, message = "size must be at most 20 characters")
        String size,

        @Size(max = 150, message = "location must be at most 150 characters")
        String location
) {
}
