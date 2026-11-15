package com.jobportal.kaamsathi.masterdata.controller;

import com.jobportal.kaamsathi.masterdata.dto.MasterItemRequest;
import com.jobportal.kaamsathi.masterdata.dto.SkillResponse;
import com.jobportal.kaamsathi.masterdata.service.SkillService;
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
 * Admin manages the skill list. Every method here is ADMIN only (class-level @PreAuthorize).
 */
@RestController
@RequestMapping("/api/masterdata/skills")
@PreAuthorize("hasRole('ADMIN')")
public class SkillAdminController {

    private final SkillService skillService;

    public SkillAdminController(SkillService skillService) {
        this.skillService = skillService;
    }

    /** GET /api/masterdata/skills/all → 200 + every skill (hidden too) */
    @GetMapping("/all")
    public List<SkillResponse> all() {
        return skillService.listAll();
    }

    /** POST /api/masterdata/skills {"name":"Java"} → 201 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse create(@Valid @RequestBody MasterItemRequest request) {
        return skillService.create(request.name());
    }

    /** PUT /api/masterdata/skills/{id} {"name":"Java SE"} → 200 */
    @PutMapping("/{id}")
    public SkillResponse rename(@PathVariable UUID id, @Valid @RequestBody MasterItemRequest request) {
        return skillService.rename(id, request.name());
    }

    /** POST /api/masterdata/skills/{id}/deactivate → 200 (hidden from dropdowns) */
    @PostMapping("/{id}/deactivate")
    public SkillResponse deactivate(@PathVariable UUID id) {
        return skillService.deactivate(id);
    }

    /** POST /api/masterdata/skills/{id}/activate → 200 (visible again) */
    @PostMapping("/{id}/activate")
    public SkillResponse activate(@PathVariable UUID id) {
        return skillService.activate(id);
    }
}
