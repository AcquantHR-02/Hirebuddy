package com.jobportal.kaamsathi.job.service;

import com.jobportal.kaamsathi.auth.service.UserService;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.company.service.CompanyService;
import com.jobportal.kaamsathi.job.dto.JobRef;
import com.jobportal.kaamsathi.job.dto.JobRequest;
import com.jobportal.kaamsathi.job.dto.JobResponse;
import com.jobportal.kaamsathi.job.entity.Job;
import com.jobportal.kaamsathi.job.entity.WorkMode;
import com.jobportal.kaamsathi.job.repository.JobRepository;
import com.jobportal.kaamsathi.masterdata.service.CityService;
import com.jobportal.kaamsathi.masterdata.service.JobCategoryService;
import com.jobportal.kaamsathi.masterdata.service.SkillService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Job rules (company side): post, edit, close, reopen, list own jobs.
 * Jobs belong to the COMPANY: any owner / recruiter of that company may manage them.
 */
@Service
public class JobService {

    static final int FREE_POST_LIMIT = 5;                       // free tier, lifetime (plans come in Phase 4)
    static final Duration JOB_VALIDITY = Duration.ofDays(60);   // auto-close scheduler comes in Phase 5

    private final JobRepository jobRepository;
    private final CompanyService companyService;          // other modules: SERVICES only
    private final UserService userService;
    private final JobCategoryService categoryService;
    private final CityService cityService;
    private final SkillService skillService;
    private final Clock clock;

    public JobService(JobRepository jobRepository, CompanyService companyService, UserService userService,
                      JobCategoryService categoryService, CityService cityService,
                      SkillService skillService, Clock clock) {
        this.jobRepository = jobRepository;
        this.companyService = companyService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.cityService = cityService;
        this.skillService = skillService;
        this.clock = clock;
    }

    /**
     * For other modules (application): the job must be LIVE (ACTIVE + not past expiry).
     * Closed / expired / blocked / unknown → 404, same answer for all (like the public job page).
     */
    @Transactional(readOnly = true)
    public JobRef requireLiveJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .filter(j -> j.isLive(clock.instant()))
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_NOT_FOUND));
        return new JobRef(job.getId(), job.getCompanyId(), job.getTitle());
    }

    /**
     * For other modules (application): the job must belong to the caller's company (any status:
     * closed / expired jobs still have applicants to manage). Not theirs / unknown → 404.
     */
    @Transactional(readOnly = true)
    public JobRef requireOwnJob(UUID userId, UUID jobId) {
        Job job = findOwnJob(userId, jobId);
        return new JobRef(job.getId(), job.getCompanyId(), job.getTitle());
    }

    /**
     * Owner or recruiter posts a job for THEIR company.
     * 1. company exists + VERIFIED (409)   2. free posts left (409)   3. category / city / skills valid (400)
     * → ACTIVE, expires in 60 days.
     */
    @Transactional
    public JobResponse create(UUID userId, JobRequest request) {
        UUID companyId = companyService.requireVerifiedCompany(userId);   // from DB, not the token

        if (jobRepository.countByCompanyId(companyId) >= FREE_POST_LIMIT) {
            throw new ApiException(ErrorCode.FREE_JOB_LIMIT_REACHED);
        }
        checkMasterdata(request);

        Instant now = clock.instant();
        Job job = new Job(
                UUID.randomUUID(),
                companyId,
                userId,
                request.title().trim(),
                request.description().trim(),
                request.categoryId(),
                cityOf(request),
                request.workMode(),
                request.jobType(),
                request.experienceMin().shortValue(),
                toShort(request.experienceMax()),
                request.salaryMin(),
                request.salaryMax(),
                request.skillIds(),
                now.plus(JOB_VALIDITY),
                now);
        jobRepository.save(job);
        return JobResponse.from(job);
    }

    // company's own jobs (all statuses), newest first. No company → 409 COMPANY_REQUIRED.
    @Transactional(readOnly = true)
    public List<JobResponse> listMine(UUID userId) {
        UUID companyId = userService.getCompanyId(userId);
        if (companyId == null) {
            throw new ApiException(ErrorCode.COMPANY_REQUIRED);
        }
        return jobRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(JobResponse::from)
                .toList();
    }

    // Edit (full replace). ACTIVE or CLOSED only; EXPIRED / BLOCKED can't be changed. Expiry stays the same.
    @Transactional
    public JobResponse update(UUID userId, UUID jobId, JobRequest request) {
        Job job = findOwnJob(userId, jobId);
        if (!job.isActive() && !job.isClosed()) {
            throw new ApiException(ErrorCode.JOB_INVALID_STATE, "Only an active or closed job can be edited");
        }
        checkMasterdata(request);

        job.update(
                request.title().trim(),
                request.description().trim(),
                request.categoryId(),
                cityOf(request),
                request.workMode(),
                request.jobType(),
                request.experienceMin().shortValue(),
                toShort(request.experienceMax()),
                request.salaryMin(),
                request.salaryMax(),
                request.skillIds(),
                clock.instant());
        jobRepository.flush();          // write now, so the response shows what is really saved
        return JobResponse.from(job);
    }

    // ACTIVE → CLOSED
    @Transactional
    public JobResponse close(UUID userId, UUID jobId) {
        Job job = findOwnJob(userId, jobId);
        if (!job.isActive()) {
            throw new ApiException(ErrorCode.JOB_INVALID_STATE, "Only an active job can be closed");
        }
        job.close(clock.instant());
        return JobResponse.from(job);
    }

    // CLOSED → ACTIVE, only while the original 60 days are not over (renew comes later)
    @Transactional
    public JobResponse reopen(UUID userId, UUID jobId) {
        Job job = findOwnJob(userId, jobId);
        Instant now = clock.instant();
        if (!job.isClosed()) {
            throw new ApiException(ErrorCode.JOB_INVALID_STATE, "Only a closed job can be reopened");
        }
        if (job.isPastExpiry(now)) {
            throw new ApiException(ErrorCode.JOB_INVALID_STATE, "This job's 60 days are over; it can't be reopened");
        }
        job.reopen(now);
        return JobResponse.from(job);
    }

    // ---- helpers ----

    // the job must belong to the user's company (from DB). Unknown id, other company or no company → 404.
    private Job findOwnJob(UUID userId, UUID jobId) {
        UUID companyId = userService.getCompanyId(userId);
        if (companyId == null) {
            throw new ApiException(ErrorCode.JOB_NOT_FOUND);
        }
        return jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_NOT_FOUND));
    }

    // ids must point to ACTIVE masterdata items. Wrong id = bad input → 400 with the field name.
    private void checkMasterdata(JobRequest request) {
        if (!categoryService.isActive(request.categoryId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "categoryId: unknown or inactive category");
        }
        if (request.workMode() != WorkMode.REMOTE && !cityService.isActive(request.cityId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "cityId: unknown or inactive city");
        }
        if (!skillService.areAllActive(request.skillIds())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "skillIds: unknown or inactive skill");
        }
    }

    // REMOTE jobs have no city: ignore one if it was sent
    private static UUID cityOf(JobRequest request) {
        return request.workMode() == WorkMode.REMOTE ? null : request.cityId();
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }
}
