package com.jobportal.kaamsathi.masterdata.service;

import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.masterdata.dto.IndustryResponse;
import com.jobportal.kaamsathi.masterdata.entity.Industry;
import com.jobportal.kaamsathi.masterdata.repository.IndustryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Industry list rules. Same rules as SkillService.
 */
@Service
public class IndustryService {

    private final IndustryRepository industryRepository;
    private final Clock clock;

    public IndustryService(IndustryRepository industryRepository, Clock clock) {
        this.industryRepository = industryRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<IndustryResponse> search(String q) {
        String text = (q == null) ? "" : q.trim();
        return industryRepository.findTop50ByActiveTrueAndNameContainingOrderByNameAsc(text).stream()
                .map(IndustryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IndustryResponse> listAll() {
        return industryRepository.findAllByOrderByNameAsc().stream()
                .map(IndustryResponse::from)
                .toList();
    }

    @Transactional
    public IndustryResponse create(String rawName) {
        String name = rawName.trim();
        if (industryRepository.existsByName(name)) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        Industry industry = new Industry(UUID.randomUUID(), name, clock.instant());
        try {
            industryRepository.saveAndFlush(industry);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        return IndustryResponse.from(industry);
    }

    @Transactional
    public IndustryResponse rename(UUID id, String rawName) {
        Industry industry = find(id);
        String name = rawName.trim();
        if (industryRepository.existsByNameAndIdNot(name, id)) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        industry.rename(name, clock.instant());
        try {
            industryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        return IndustryResponse.from(industry);
    }

    @Transactional
    public IndustryResponse activate(UUID id) {
        Industry industry = find(id);
        industry.activate(clock.instant());
        return IndustryResponse.from(industry);
    }

    @Transactional
    public IndustryResponse deactivate(UUID id) {
        Industry industry = find(id);
        industry.deactivate(clock.instant());
        return IndustryResponse.from(industry);
    }

    private Industry find(UUID id) {
        return industryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MASTERDATA_NOT_FOUND));
    }
}
