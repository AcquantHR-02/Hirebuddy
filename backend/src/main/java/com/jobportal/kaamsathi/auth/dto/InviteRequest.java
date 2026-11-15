package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/auth/invites (owner invites a recruiter).
 */
public record InviteRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email is not valid")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email
) {
}
