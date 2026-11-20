package com.jobportal.kaamsathi.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Text fields of the owner's "I paid" form. Sent as multipart/form-data together with
 * an OPTIONAL file part "screenshot" (that part is read separately in the controller).
 */
public record SubmitPaymentRequest(

        @NotNull(message = "planId is required")
        UUID planId,

        @NotBlank(message = "transactionId is required")
        @Size(max = 100, message = "transactionId must be at most 100 characters")
        String transactionId
) {
}
