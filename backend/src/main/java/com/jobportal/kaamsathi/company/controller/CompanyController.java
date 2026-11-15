package com.jobportal.kaamsathi.company.controller;

import com.jobportal.kaamsathi.auth.dto.UserResponse;
import com.jobportal.kaamsathi.company.dto.CompanyResponse;
import com.jobportal.kaamsathi.company.dto.RegisterCompanyRequest;
import com.jobportal.kaamsathi.company.dto.TransferOwnershipRequest;
import com.jobportal.kaamsathi.company.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Company endpoints. HTTP only; rules live in CompanyService.
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /** POST /api/companies → 201 + company (PENDING). COMPANY_OWNER only; others get 403. */
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse register(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody RegisterCompanyRequest request) {
        // owner id comes from the signed token, never from the body (nobody can register for someone else)
        return companyService.register(UUID.fromString(jwt.getSubject()), request);
    }

    /** GET /api/companies/me → 200 + the owner's company (status, rejection reason). No company → 404. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    public CompanyResponse myCompany(@AuthenticationPrincipal Jwt jwt) {
        // looked up by owner id (not the token's companyId claim: that is only fresh after a refresh)
        return companyService.getByOwner(UUID.fromString(jwt.getSubject()));
    }

    /** PUT /api/companies/me → 200. Edit details; a REJECTED company is resubmitted (→ PENDING). VERIFIED → 409. */
    @PutMapping("/me")
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    public CompanyResponse updateMyCompany(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody RegisterCompanyRequest request) {
        return companyService.update(UUID.fromString(jwt.getSubject()), request);
    }

    /** GET /api/companies/me/recruiters → 200 + the owner's team (RECRUITERs) */
    @GetMapping("/me/recruiters")
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    public List<UserResponse> myRecruiters(@AuthenticationPrincipal Jwt jwt) {
        return companyService.listRecruiters(UUID.fromString(jwt.getSubject()));
    }

    /** DELETE /api/companies/me/recruiters/{userId} → 204. Detaches the recruiter (account stays). */
    @DeleteMapping("/me/recruiters/{userId}")
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRecruiter(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        companyService.removeRecruiter(UUID.fromString(jwt.getSubject()), userId);
    }

    /** POST /api/companies/me/transfer-ownership → 200 + company (new ownerId). Both users must log in again. */
    @PostMapping("/me/transfer-ownership")
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    public CompanyResponse transferOwnership(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody TransferOwnershipRequest request) {
        return companyService.transferOwnership(
                UUID.fromString(jwt.getSubject()), request.newOwnerId(), request.password());
    }
}
