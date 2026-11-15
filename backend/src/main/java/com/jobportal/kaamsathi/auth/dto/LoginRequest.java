package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of POST /api/auth/login.
 * No password length rule here: login only compares; length rules belong to signup.
 */
public record LoginRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email is not valid")
        String email,

        @NotBlank(message = "password is required")
        String password
) {
}
