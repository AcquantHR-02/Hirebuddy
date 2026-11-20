package com.jobportal.kaamsathi.job.controller;

import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.job.dto.JobDetailResponse;
import com.jobportal.kaamsathi.job.dto.JobSearchFilter;
import com.jobportal.kaamsathi.job.dto.JobSummaryResponse;
import com.jobportal.kaamsathi.job.entity.JobType;
import com.jobportal.kaamsathi.job.entity.WorkMode;
import com.jobportal.kaamsathi.job.service.JobSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * PUBLIC job endpoints (no login): search + job page. Opened in SecurityConfig.
 * A wrong type in the URL (?workMode=OFFICE, bad UUID) → 400 from GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/jobs")
public class JobSearchController {

    private final JobSearchService jobSearchService;

    public JobSearchController(JobSearchService jobSearchService) {
        this.jobSearchService = jobSearchService;
    }

    /** GET /api/jobs?q=java&cityId=&categoryId=&skillId=&workMode=&jobType=&experience=&salaryMin=&page=0&size=20 */
    @GetMapping
    public PageResponse<JobSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID skillId,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) Integer experience,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return jobSearchService.search(new JobSearchFilter(
                q, cityId, categoryId, skillId, workMode, jobType, experience, salaryMin, page, size));
    }

    /** GET /api/jobs/{id} → 200 job page; not live (closed / expired / unknown) → 404. */
    @GetMapping("/{id}")
    public JobDetailResponse getPublic(@PathVariable UUID id) {
        return jobSearchService.getPublic(id);
    }
}
