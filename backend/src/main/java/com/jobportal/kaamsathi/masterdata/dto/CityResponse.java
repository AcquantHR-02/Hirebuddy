package com.jobportal.kaamsathi.masterdata.dto;

import com.jobportal.kaamsathi.masterdata.entity.City;

import java.util.UUID;

/**
 * What we send back about a city.
 */
public record CityResponse(
        UUID id,
        String name,
        String state,
        boolean active
) {

    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getState(), city.isActive());
    }
}
