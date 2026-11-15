package com.jobportal.kaamsathi.masterdata.service;

import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.masterdata.dto.CityResponse;
import com.jobportal.kaamsathi.masterdata.entity.City;
import com.jobportal.kaamsathi.masterdata.repository.CityRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * City list rules. Same as SkillService, but a duplicate means the same (name, state) PAIR.
 */
@Service
public class CityService {

    private final CityRepository cityRepository;
    private final Clock clock;

    public CityService(CityRepository cityRepository, Clock clock) {
        this.cityRepository = cityRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CityResponse> search(String q) {
        String text = (q == null) ? "" : q.trim();
        return cityRepository.findTop50ByActiveTrueAndNameContainingOrderByNameAscStateAsc(text).stream()
                .map(CityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CityResponse> listAll() {
        return cityRepository.findAllByOrderByNameAscStateAsc().stream()
                .map(CityResponse::from)
                .toList();
    }

    @Transactional
    public CityResponse create(String rawName, String rawState) {
        String name = rawName.trim();
        String state = rawState.trim();
        if (cityRepository.existsByNameAndState(name, state)) {    // same city in the SAME state?
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        City city = new City(UUID.randomUUID(), name, state, clock.instant());
        try {
            cityRepository.saveAndFlush(city);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        return CityResponse.from(city);
    }

    @Transactional
    public CityResponse update(UUID id, String rawName, String rawState) {
        City city = find(id);
        String name = rawName.trim();
        String state = rawState.trim();
        if (cityRepository.existsByNameAndStateAndIdNot(name, state, id)) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        city.update(name, state, clock.instant());
        try {
            cityRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        return CityResponse.from(city);
    }

    @Transactional
    public CityResponse activate(UUID id) {
        City city = find(id);
        city.activate(clock.instant());
        return CityResponse.from(city);
    }

    @Transactional
    public CityResponse deactivate(UUID id) {
        City city = find(id);
        city.deactivate(clock.instant());
        return CityResponse.from(city);
    }

    private City find(UUID id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MASTERDATA_NOT_FOUND));
    }
}
