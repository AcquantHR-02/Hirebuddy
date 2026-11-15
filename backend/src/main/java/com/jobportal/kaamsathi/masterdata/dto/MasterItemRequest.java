package com.jobportal.kaamsathi.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body for adding or renaming a skill / industry / job category (admin): {"name": "Java"}.
 * One shape for all three lists, so one class.
 */
public record MasterItemRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name
) {
}
