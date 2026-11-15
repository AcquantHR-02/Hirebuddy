package com.jobportal.hirebuddy.company.service;

import com.jobportal.hirebuddy.auth.dto.UserResponse;
import com.jobportal.hirebuddy.auth.service.CompanyVerificationCheck;
import com.jobportal.hirebuddy.auth.service.UserService;
import com.jobportal.hirebuddy.common.exception.ApiException;
import com.jobportal.hirebuddy.common.exception.ErrorCode;
import com.jobportal.hirebuddy.company.dto.CompanyResponse;
import com.jobportal.hirebuddy.company.dto.RegisterCompanyRequest;
import com.jobportal.hirebuddy.company.entity.Company;
import com.jobportal.hirebuddy.company.entity.CompanyStatus;
import com.jobportal.hirebuddy.company.repository.CompanyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Company rules: register (owner), later verify / reject (admin).
 */
@Service
public class CompanyService implements CompanyVerificationCheck {

    private final CompanyRepository companyRepository;
    private final UserService userService;      // auth's SERVICE, never its repository
    private final Clock clock;

    public CompanyService(CompanyRepository companyRepository, UserService userService, Clock clock) {
        this.companyRepository = companyRepository;
        this.userService = userService;
        this.clock = clock;
    }

    // Owner registers their ONE company → PENDING. Company row + users.company_id in ONE transaction.
    @Transactional
    public CompanyResponse register(UUID ownerId, RegisterCompanyRequest request) {
        if (companyRepository.findByOwnerId(ownerId).isPresent()) {
            throw new ApiException(ErrorCode.COMPANY_ALREADY_EXISTS);
        }
        String gstin = clean(request.gstin());
        if (gstin != null && companyRepository.existsByGstin(gstin)) {
            throw new ApiException(ErrorCode.GSTIN_ALREADY_EXISTS);
        }

        Company company = new Company(
                UUID.randomUUID(),
                ownerId,
                request.name().trim(),
                gstin,
                clean(request.website()),
                clean(request.industry()),
                clean(request.size()),
                clean(request.location()),
                clock.instant());

        try {
            companyRepository.saveAndFlush(company);   // INSERT now, so a UNIQUE clash fails inside try
        } catch (DataIntegrityViolationException e) {
            // two requests at the same moment: the DB UNIQUE keys stop the 2nd one
            String msg = String.valueOf(e.getMostSpecificCause().getMessage());
            throw new ApiException(msg.contains("uk_companies_gstin")
                    ? ErrorCode.GSTIN_ALREADY_EXISTS
                    : ErrorCode.COMPANY_ALREADY_EXISTS);
        }

        userService.assignCompany(ownerId, company.getId());   // same transaction: both saved or neither
        return CompanyResponse.from(company);
    }

    // owner's own company (status + rejection reason). No company yet → 404.
    @Transactional(readOnly = true)
    public CompanyResponse getByOwner(UUID ownerId) {
        return companyRepository.findByOwnerId(ownerId)
                .map(CompanyResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
    }

    // Owner edits their company (full replace). PENDING stays PENDING; REJECTED → PENDING (resubmit).
    // VERIFIED can't be edited for now (it would skip the admin review).
    @Transactional
    public CompanyResponse update(UUID ownerId, RegisterCompanyRequest request) {
        Company company = companyRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
        if (company.isVerified()) {
            throw new ApiException(ErrorCode.COMPANY_ALREADY_VERIFIED);
        }
        String gstin = clean(request.gstin());
        if (gstin != null && companyRepository.existsByGstinAndIdNot(gstin, company.getId())) {
            throw new ApiException(ErrorCode.GSTIN_ALREADY_EXISTS);
        }

        company.updateDetails(
                request.name().trim(),
                gstin,
                clean(request.website()),
                clean(request.industry()),
                clean(request.size()),
                clean(request.location()),
                clock.instant());

        try {
            companyRepository.flush();   // UPDATE now, so a GSTIN race fails inside try
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.GSTIN_ALREADY_EXISTS);
        }
        return CompanyResponse.from(company);
    }

    // ---- team (owner) ----

    @Transactional(readOnly = true)
    public List<UserResponse> listRecruiters(UUID ownerId) {
        return userService.listRecruiters(ownerCompanyId(ownerId));
    }

    @Transactional
    public void removeRecruiter(UUID ownerId, UUID recruiterId) {
        userService.removeRecruiter(ownerCompanyId(ownerId), recruiterId);
    }

    // Owner hands the company to one of its recruiters. Roles (auth) + owner_id (company) in ONE transaction.
    @Transactional
    public CompanyResponse transferOwnership(UUID ownerId, UUID newOwnerId, String password) {
        Company company = companyRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
        userService.transferOwnership(company.getId(), ownerId, password, newOwnerId);
        company.changeOwner(newOwnerId, clock.instant());
        return CompanyResponse.from(company);
    }

    // the owner's company id from the DB (not the token claim). No company → 404.
    private UUID ownerCompanyId(UUID ownerId) {
        return companyRepository.findByOwnerId(ownerId)
                .map(Company::getId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
    }

    // for auth (CompanyVerificationCheck): may this company build a team? unknown id → false
    @Override
    @Transactional(readOnly = true)
    public boolean isVerified(UUID companyId) {
        return companyRepository.findById(companyId)
                .map(Company::isVerified)
                .orElse(false);
    }

    // ---- admin review ----

    // review queue: all PENDING companies, oldest first
    @Transactional(readOnly = true)
    public List<CompanyResponse> listPending() {
        return companyRepository.findByStatusOrderByCreatedAtAsc(CompanyStatus.PENDING).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    // PENDING → VERIFIED (company may post jobs)
    @Transactional
    public CompanyResponse verify(UUID companyId) {
        Company company = findPending(companyId);
        company.verify(clock.instant());              // dirty checking writes the UPDATE
        return CompanyResponse.from(company);
    }

    // PENDING → REJECTED with a reason the owner will see
    @Transactional
    public CompanyResponse reject(UUID companyId, String reason) {
        Company company = findPending(companyId);
        company.reject(reason.trim(), clock.instant());
        return CompanyResponse.from(company);
    }

    // unknown id → 404; already reviewed → 409 (no double verify / reject)
    private Company findPending(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
        if (!company.isPending()) {
            throw new ApiException(ErrorCode.COMPANY_NOT_PENDING);
        }
        return company;
    }

    // optional text: trim it, and treat "" / "   " as not given (null)
    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
