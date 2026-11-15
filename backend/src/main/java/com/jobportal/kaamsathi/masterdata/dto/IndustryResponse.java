package com.jobportal.kaamsathi.masterdata.dto;

import com.jobportal.kaamsathi.masterdata.entity.Industry;

import java.util.UUID;

/**
 * What we send back about an industry.
 */
public record IndustryResponse(
        UUID id,
        String name,
        boolean active
) {

    public static IndustryResponse from(Industry industry) {
        return new IndustryResponse(industry.getId(), industry.getName(), industry.isActive());
    }
}
