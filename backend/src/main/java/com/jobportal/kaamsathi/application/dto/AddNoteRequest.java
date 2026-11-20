package com.jobportal.kaamsathi.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Private note on an application: {"text": "Strong Spring Boot, weak SQL"}.
 */
public record AddNoteRequest(

        @NotBlank(message = "text is required")
        @Size(max = 2000, message = "text must be at most 2000 characters")
        String text
) {
}
