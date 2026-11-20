package com.jobportal.kaamsathi.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Company's private rating: {"rating": 4}. {"rating": null} (or {}) clears it.
 * Wrapper Integer (not int) so "no rating" is possible; @Min / @Max skip null.
 */
public record RateRequest(

        @Min(value = 1, message = "rating must be between 1 and 5")
        @Max(value = 5, message = "rating must be between 1 and 5")
        Integer rating
) {
}
