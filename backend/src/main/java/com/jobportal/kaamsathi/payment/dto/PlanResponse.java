package com.jobportal.kaamsathi.payment.dto;

import com.jobportal.kaamsathi.payment.entity.Plan;

import java.util.UUID;

/**
 * What the API returns for a plan (public "choose a plan" page + admin screen).
 * No timestamps: the frontend doesn't need them.
 */
public record PlanResponse(
        UUID id,
        String name,
        int priceRupees,
        int durationDays,
        int jobSlots,
        int recruiterSeats,
        boolean active
) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getPriceRupees(),
                plan.getDurationDays(),
                plan.getJobSlots(),
                plan.getRecruiterSeats(),
                plan.isActive()
        );
    }
}
