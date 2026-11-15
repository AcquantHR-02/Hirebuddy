package com.jobportal.kaamsathi.masterdata.controller;

import com.jobportal.kaamsathi.masterdata.dto.IndustryResponse;
import com.jobportal.kaamsathi.masterdata.dto.MasterItemRequest;
import com.jobportal.kaamsathi.masterdata.service.IndustryService;
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
 * Admin manages the industry list. ADMIN only (class-level @PreAuthorize).
 */
@RestController
@RequestMapping("/api/masterdata/industries")
@PreAuthorize("hasRole('ADMIN')")
public class IndustryAdminController {

    private final IndustryService industryService;

    public IndustryAdminController(IndustryService industryService) {
        this.industryService = industryService;
    }

    @GetMapping("/all")
    public List<IndustryResponse> all() {
        return industryService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IndustryResponse create(@Valid @RequestBody MasterItemRequest request) {
        return industryService.create(request.name());
    }

    @PutMapping("/{id}")
    public IndustryResponse rename(@PathVariable UUID id, @Valid @RequestBody MasterItemRequest request) {
        return industryService.rename(id, request.name());
    }

    @PostMapping("/{id}/deactivate")
    public IndustryResponse deactivate(@PathVariable UUID id) {
        return industryService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public IndustryResponse activate(@PathVariable UUID id) {
        return industryService.activate(id);
    }
}
