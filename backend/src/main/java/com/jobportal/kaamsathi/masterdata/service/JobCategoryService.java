package com.jobportal.kaamsathi.masterdata.service;

import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.masterdata.dto.JobCategoryResponse;
import com.jobportal.kaamsathi.masterdata.entity.JobCategory;
import com.jobportal.kaamsathi.masterdata.repository.JobCategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Job category list rules. Same rules as SkillService.
 */
@Service
public class JobCategoryService {

    private final JobCategoryRepository categoryRepository;
    private final Clock clock;

    public JobCategoryService(JobCategoryRepository categoryRepository, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<JobCategoryResponse> search(String q) {
        String text = (q == null) ? "" : q.trim();
        return categoryRepository.findTop50ByActiveTrueAndNameContainingOrderByNameAsc(text).stream()
                .map(JobCategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobCategoryResponse> listAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(JobCategoryResponse::from)
                .toList();
    }

    @Transactional
    public JobCategoryResponse create(String rawName) {
        String name = rawName.trim();
        if (categoryRepository.existsByName(name)) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        JobCategory category = new JobCategory(UUID.randomUUID(), name, clock.instant());
        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        return JobCategoryResponse.from(category);
    }

    @Transactional
    public JobCategoryResponse rename(UUID id, String rawName) {
        JobCategory category = find(id);
        String name = rawName.trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        category.rename(name, clock.instant());
        try {
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.MASTERDATA_NAME_EXISTS);
        }
        return JobCategoryResponse.from(category);
    }

    @Transactional
    public JobCategoryResponse activate(UUID id) {
        JobCategory category = find(id);
        category.activate(clock.instant());
        return JobCategoryResponse.from(category);
    }

    @Transactional
    public JobCategoryResponse deactivate(UUID id) {
        JobCategory category = find(id);
        category.deactivate(clock.instant());
        return JobCategoryResponse.from(category);
    }

    private JobCategory find(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.MASTERDATA_NOT_FOUND));
    }
}
