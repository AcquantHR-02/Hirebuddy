package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of DELETE /api/auth/me: the password again, so a stolen access token alone can't delete the account.
 */
public record DeleteAccountRequest(

        @NotBlank(message = "password is required")
        String password
) {
}
