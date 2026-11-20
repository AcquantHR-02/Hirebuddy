package com.jobportal.kaamsathi.job.service;

import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.util.Paging;
import com.jobportal.kaamsathi.job.dto.JobSummaryResponse;
import com.jobportal.kaamsathi.job.dto.SavedJobResponse;
import com.jobportal.kaamsathi.job.entity.Job;
import com.jobportal.kaamsathi.job.entity.JobStatus;
import com.jobportal.kaamsathi.job.entity.SavedJob;
import com.jobportal.kaamsathi.job.entity.SavedJobId;
import com.jobportal.kaamsathi.job.repository.JobRepository;
import com.jobportal.kaamsathi.job.repository.SavedJobRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seeker's saved jobs. Save / unsave are IDEMPOTENT: doing it twice gives the same result (204), never an error.
 */
@Service
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final JobSearchService jobSearchService;     // same module: builds the job cards
    private final Clock clock;

    public SavedJobService(SavedJobRepository savedJobRepository, JobRepository jobRepository,
                           JobSearchService jobSearchService, Clock clock) {
        this.savedJobRepository = savedJobRepository;
        this.jobRepository = jobRepository;
        this.jobSearchService = jobSearchService;
        this.clock = clock;
    }

    // only a LIVE job can be saved (closed / expired / unknown → 404). Already saved → nothing changes.
    @Transactional
    public void save(UUID seekerId, UUID jobId) {
        Instant now = clock.instant();
        jobRepository.findByIdAndStatusAndExpiresAtAfter(jobId, JobStatus.ACTIVE, now)
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_NOT_FOUND));

        SavedJobId id = new SavedJobId(seekerId, jobId);
        if (savedJobRepository.existsById(id)) {
            return;
        }
        try {
            savedJobRepository.saveAndFlush(new SavedJob(id, now));
        } catch (DataIntegrityViolationException e) {
            // two clicks at the same moment: the PRIMARY KEY stopped the 2nd insert → already saved = fine
        }
    }

    // remove from the list; not saved (or unknown job) → still fine
    @Transactional
    public void unsave(UUID seekerId, UUID jobId) {
        savedJobRepository.deleteById(new SavedJobId(seekerId, jobId));
    }

    // newest saved first; closed / expired jobs stay in the list with live = false
    @Transactional(readOnly = true)
    public PageResponse<SavedJobResponse> list(UUID seekerId, Integer page, Integer size) {
        Instant now = clock.instant();
        Page<SavedJob> saved = savedJobRepository.findByIdSeekerIdOrderByCreatedAtDesc(
                seekerId, Paging.of(page, size));

        // load all jobs of this page in ONE query, then their cards (names in 4 more queries)
        List<UUID> jobIds = saved.getContent().stream().map(s -> s.getId().jobId()).toList();
        Map<UUID, Job> jobs = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        Map<UUID, JobSummaryResponse> cards = jobSearchService.toSummaries(List.copyOf(jobs.values()));

        return PageResponse.from(saved, s -> {
            UUID jobId = s.getId().jobId();
            return new SavedJobResponse(cards.get(jobId), jobs.get(jobId).isLive(now), s.getCreatedAt());
        });
    }
}
