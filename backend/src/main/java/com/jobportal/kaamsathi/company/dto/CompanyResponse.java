package com.jobportal.kaamsathi.company.dto;

import com.jobportal.kaamsathi.company.entity.Company;
import com.jobportal.kaamsathi.company.entity.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * What we send back about a company (to its owner and to the admin).
 */
public record CompanyResponse(
        UUID id,
        UUID ownerId,
        String name,
        String gstin,
        String website,
        String industry,
        String size,
        String location,
        CompanyStatus status,
        String rejectionReason,   // null unless REJECTED
        Instant createdAt
) {

    // entity → DTO (called inside the service, so the entity never leaves it)
    public static CompanyResponse from(Company c) {
        return new CompanyResponse(
                c.getId(),
                c.getOwnerId(),
                c.getName(),
                c.getGstin(),
                c.getWebsite(),
                c.getIndustry(),
                c.getSize(),
                c.getLocation(),
                c.getStatus(),
                c.getRejectionReason(),
                c.getCreatedAt());
    }
}
