package com.jobportal.kaamsathi.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body for adding / updating a city (admin): {"name": "Pune", "state": "Maharashtra"}.
 */
public record CityRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "state is required")
        @Size(max = 100, message = "state must be at most 100 characters")
        String state
) {
}
