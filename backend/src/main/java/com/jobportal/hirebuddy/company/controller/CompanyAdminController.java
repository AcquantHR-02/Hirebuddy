package com.jobportal.hirebuddy.company.controller;

import com.jobportal.hirebuddy.company.dto.CompanyResponse;
import com.jobportal.hirebuddy.company.dto.RejectCompanyRequest;
import com.jobportal.hirebuddy.company.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin review of companies. Every method here is ADMIN only (class-level @PreAuthorize).
 */
@RestController
@RequestMapping("/api/companies")
@PreAuthorize("hasRole('ADMIN')")
public class CompanyAdminController {

    private final CompanyService companyService;

    public CompanyAdminController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /** GET /api/companies/pending → 200 + review queue (oldest first) */
    @GetMapping("/pending")
    public List<CompanyResponse> pending() {
        return companyService.listPending();
    }

    /** POST /api/companies/{id}/verify → 200 + company (VERIFIED) */
    @PostMapping("/{id}/verify")
    public CompanyResponse verify(@PathVariable UUID id) {
        return companyService.verify(id);
    }

    /** POST /api/companies/{id}/reject {"reason": "..."} → 200 + company (REJECTED) */
    @PostMapping("/{id}/reject")
    public CompanyResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectCompanyRequest request) {
        return companyService.reject(id, request.reason());
    }
}
