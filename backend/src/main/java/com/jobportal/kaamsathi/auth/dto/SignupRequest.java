package com.jobportal.kaamsathi.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON body of POST /api/auth/signup/seeker and /api/auth/signup/company (same fields).
 * The rules below are checked before the service runs (@Valid in the controller).
 */
public record SignupRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email is not valid")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")   // BCrypt uses only the first 72 bytes
        String password,

        // Boolean (not boolean): Jackson 3 rejects a missing primitive as broken JSON.
        // @NotNull catches a missing field, @AssertTrue catches false.
        @NotNull(message = "you must accept the terms")
        @AssertTrue(message = "you must accept the terms")
        Boolean consent
) {
}
