package com.jobportal.kaamsathi.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * JSON body of POST /api/companies/me/transfer-ownership.
 * Password asked again: giving away a company is a sensitive action.
 */
public record TransferOwnershipRequest(

        @NotNull(message = "newOwnerId is required")
        UUID newOwnerId,

        @NotBlank(message = "password is required")
        String password
) {
}
