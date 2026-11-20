package com.jobportal.kaamsathi.payment.controller;

import com.jobportal.kaamsathi.payment.dto.PlanRequest;
import com.jobportal.kaamsathi.payment.dto.PlanResponse;
import com.jobportal.kaamsathi.payment.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin manages plans. Every method here is ADMIN only (class-level @PreAuthorize).
 */
@RestController
@RequestMapping("/api/admin/plans")
@PreAuthorize("hasRole('ADMIN')")
public class PlanAdminController {

    private final PlanService planService;

    public PlanAdminController(PlanService planService) {
        this.planService = planService;
    }

    /** GET /api/admin/plans → 200 + every plan (hidden too) */
    @GetMapping
    public List<PlanResponse> all() {
        return planService.listAll();
    }

    /** POST /api/admin/plans {name, priceRupees, durationDays, jobSlots, recruiterSeats} → 201 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse create(@Valid @RequestBody PlanRequest request) {
        return planService.create(request);
    }

    /** PUT /api/admin/plans/{id} (same body, full replace) → 200 */
    @PutMapping("/{id}")
    public PlanResponse update(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        return planService.update(id, request);
    }

    /** POST /api/admin/plans/{id}/deactivate → 200 (can't be bought anymore) */
    @PostMapping("/{id}/deactivate")
    public PlanResponse deactivate(@PathVariable UUID id) {
        return planService.deactivate(id);
    }

    /** POST /api/admin/plans/{id}/activate → 200 (can be bought again) */
    @PostMapping("/{id}/activate")
    public PlanResponse activate(@PathVariable UUID id) {
        return planService.activate(id);
    }
}
