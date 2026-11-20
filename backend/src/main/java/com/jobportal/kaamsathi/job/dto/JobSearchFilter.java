package com.jobportal.kaamsathi.job.dto;

import com.jobportal.kaamsathi.job.entity.JobType;
import com.jobportal.kaamsathi.job.entity.WorkMode;

import java.util.UUID;

/**
 * Search filters from the URL (?q=java&cityId=...&page=0&size=20). Every field is optional (null = no filter).
 * experience = seeker's years; salaryMin = lowest yearly salary the seeker wants.
 */
public record JobSearchFilter(
        String q,
        UUID cityId,
        UUID categoryId,
        UUID skillId,
        WorkMode workMode,
        JobType jobType,
        Integer experience,
        Integer salaryMin,
        Integer page,
        Integer size
) {
}
