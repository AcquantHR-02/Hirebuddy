package com.jobportal.kaamsathi.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON body for admin "add plan" (POST) and "edit plan" (PUT, full replace):
 * {"name": "Pro", "priceRupees": 999, "durationDays": 30, "jobSlots": 10, "recruiterSeats": 3}.
 * Numbers are Integer (not int) so a MISSING field gives a clear VALIDATION_FAILED, not MALFORMED_JSON.
 * Limits match the CHECKs in V17__create_plans.sql.
 */
public record PlanRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "priceRupees is required")
        @Min(value = 1, message = "priceRupees must be at least 1")
        @Max(value = 10_000_000, message = "priceRupees must be at most 10000000")
        Integer priceRupees,

        @NotNull(message = "durationDays is required")
        @Min(value = 1, message = "durationDays must be at least 1")
        @Max(value = 3650, message = "durationDays must be at most 3650")
        Integer durationDays,

        @NotNull(message = "jobSlots is required")
        @Min(value = 1, message = "jobSlots must be at least 1")
        @Max(value = 1000, message = "jobSlots must be at most 1000")
        Integer jobSlots,

        @NotNull(message = "recruiterSeats is required")
        @Min(value = 1, message = "recruiterSeats must be at least 1")
        @Max(value = 1000, message = "recruiterSeats must be at most 1000")
        Integer recruiterSeats
) {
}
