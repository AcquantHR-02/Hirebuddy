package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/auth/invites/accept: token from the invite link + the recruiter's new password.
 * No email field: the email comes from the invite itself.
 */
public record AcceptInviteRequest(

        @NotBlank(message = "token is required")
        String token,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
        String password,

        // same DPDP consent as signup
        @NotNull(message = "you must accept the terms")
        @AssertTrue(message = "you must accept the terms")
        Boolean consent
) {
}
