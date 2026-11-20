package com.jobportal.kaamsathi.application.service;

import com.jobportal.kaamsathi.application.dto.ApplicantSummaryResponse;
import com.jobportal.kaamsathi.application.dto.ApplicationDetailResponse;
import com.jobportal.kaamsathi.application.dto.ApplicationResponse;
import com.jobportal.kaamsathi.application.dto.CompanyApplicationResponse;
import com.jobportal.kaamsathi.application.dto.NoteResponse;
import com.jobportal.kaamsathi.application.dto.TimelineItem;
import com.jobportal.kaamsathi.application.entity.Application;
import com.jobportal.kaamsathi.application.entity.ApplicationNote;
import com.jobportal.kaamsathi.application.entity.ApplicationStatus;
import com.jobportal.kaamsathi.application.entity.ApplicationStatusHistory;
import com.jobportal.kaamsathi.application.repository.ApplicationNoteRepository;
import com.jobportal.kaamsathi.application.repository.ApplicationRepository;
import com.jobportal.kaamsathi.application.repository.ApplicationStatusHistoryRepository;
import com.jobportal.kaamsathi.auth.service.UserService;
import com.jobportal.kaamsathi.file.dto.FileContent;
import com.jobportal.kaamsathi.file.service.FileService;
import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.util.Paging;
import com.jobportal.kaamsathi.job.dto.JobRef;
import com.jobportal.kaamsathi.job.dto.JobSummaryResponse;
import com.jobportal.kaamsathi.job.service.JobSearchService;
import com.jobportal.kaamsathi.job.service.JobService;
import com.jobportal.kaamsathi.seeker.dto.ApplicantData;
import com.jobportal.kaamsathi.seeker.service.SeekerProfileService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Applications. Seeker side: apply, my applications, withdraw, timeline.
 * Company side: applicants list, open (VIEWED), move stages, private notes + rating, resume.
 * Other modules are reached through their SERVICES only (job, seeker, auth, file).
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationNoteRepository noteRepository;
    private final JobService jobService;
    private final JobSearchService jobSearchService;
    private final SeekerProfileService seekerProfileService;
    private final UserService userService;
    private final FileService fileService;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ApplicationStatusHistoryRepository historyRepository,
                              ApplicationNoteRepository noteRepository, JobService jobService,
                              JobSearchService jobSearchService, SeekerProfileService seekerProfileService,
                              UserService userService, FileService fileService, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.noteRepository = noteRepository;
        this.jobService = jobService;
        this.jobSearchService = jobSearchService;
        this.seekerProfileService = seekerProfileService;
        this.userService = userService;
        this.fileService = fileService;
        this.clock = clock;
    }

    /**
     * Apply: 1. job live (404)  2. not applied before, withdrawn included (409)
     * 3. profile + resume ready (409)  → APPLIED with a frozen profile copy + resume version.
     */
    @Transactional
    public ApplicationResponse apply(UUID seekerId, UUID jobId) {
        JobRef job = jobService.requireLiveJob(jobId);
        if (applicationRepository.existsByJobIdAndSeekerId(jobId, seekerId)) {
            throw new ApiException(ErrorCode.ALREADY_APPLIED);
        }
        ApplicantData data = seekerProfileService.snapshotForApply(seekerId);

        Application application = new Application(UUID.randomUUID(), job.id(), job.companyId(), seekerId,
                data.resumeFileId(), data.snapshot(), clock.instant());
        try {
            applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            // two clicks at the same moment: UNIQUE (job_id, seeker_id) stopped the 2nd one
            throw new ApiException(ErrorCode.ALREADY_APPLIED);
        }
        recordHistory(application, null, seekerId);              // timeline step 1: APPLIED
        return ApplicationResponse.from(application, card(jobId));
    }

    /** My applications, newest first, with job cards (closed jobs included). */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listMine(UUID seekerId, Integer page, Integer size) {
        Page<Application> apps = applicationRepository.findBySeekerIdOrderByCreatedAtDesc(
                seekerId, Paging.of(page, size));
        List<UUID> jobIds = apps.getContent().stream().map(Application::getJobId).toList();
        Map<UUID, JobSummaryResponse> cards = jobSearchService.cardsByIds(jobIds);   // few queries per PAGE
        return PageResponse.from(apps, a -> ApplicationResponse.from(a, cards.get(a.getJobId())));
    }

    /** Withdraw MY application. Not mine / unknown → 404; already finished → 409. */
    @Transactional
    public ApplicationResponse withdraw(UUID seekerId, UUID applicationId) {
        // locked: a recruiter moving it at the same moment waits (no lost update)
        Application application = applicationRepository.findForUpdateByIdAndSeekerId(applicationId, seekerId)
                .orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND));
        ApplicationStatus before = application.getStatus();
        if (!application.withdraw(clock.instant())) {
            throw new ApiException(ErrorCode.APPLICATION_INVALID_STATE,
                    "This application is already " + application.getStatus());
        }
        recordHistory(application, before, seekerId);
        return ApplicationResponse.from(application, card(application.getJobId()));
    }

    /** One of MY applications + its timeline (oldest first). Not mine / unknown → 404. */
    @Transactional(readOnly = true)
    public ApplicationDetailResponse getMine(UUID seekerId, UUID applicationId) {
        Application application = applicationRepository.findByIdAndSeekerId(applicationId, seekerId)
                .orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND));
        return new ApplicationDetailResponse(
                ApplicationResponse.from(application, card(application.getJobId())), timelineOf(applicationId));
    }

    // ---- company side (owner / recruiter) ----

    /**
     * Applicants of one of MY company's jobs, newest first, optional status filter.
     * Job not in my company / unknown → 404. Listing does NOT mark anything as VIEWED (only opening one does).
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicantSummaryResponse> listForJob(UUID userId, UUID jobId, ApplicationStatus status,
                                                             Integer page, Integer size) {
        JobRef job = jobService.requireOwnJob(userId, jobId);
        Page<Application> apps = applicationRepository.findForJob(job.id(), status, Paging.of(page, size));
        return PageResponse.from(apps, ApplicantSummaryResponse::from);
    }

    /**
     * Open one application of MY company. The FIRST open turns APPLIED → VIEWED (+ viewed_at + timeline row).
     * Opening again changes nothing. Not my company's / unknown / I have no company → 404.
     */
    @Transactional
    public CompanyApplicationResponse getForCompany(UUID userId, UUID applicationId) {
        UUID companyId = myCompanyId(userId);
        // atomic: only the very first open gets 1 (race-safe, see repository)
        boolean firstView = applicationRepository.markViewed(applicationId, companyId, clock.instant()) == 1;

        Application application = applicationRepository.findByIdAndCompanyId(applicationId, companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND));
        if (firstView) {
            recordHistory(application, ApplicationStatus.APPLIED, userId);
        }
        return CompanyApplicationResponse.from(application, timelineOf(applicationId), notesOf(applicationId));
    }

    /**
     * Recruiter moves an application of MY company.
     * 400 status not settable by hand → 404 not mine → 409 final / backward / same → history row by me.
     * Row is LOCKED while we decide, so a parallel withdraw can't be overwritten.
     */
    @Transactional
    public CompanyApplicationResponse changeStatus(UUID userId, UUID applicationId, ApplicationStatus target) {
        if (!target.isRecruiterTarget()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "status: must be one of SHORTLISTED, INTERVIEW, OFFERED, HIRED, REJECTED");
        }
        Application application = applicationRepository
                .findForUpdateByIdAndCompanyId(applicationId, myCompanyId(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND));

        ApplicationStatus before = application.getStatus();
        if (!application.moveTo(target, clock.instant())) {
            throw new ApiException(ErrorCode.APPLICATION_INVALID_STATE, before.isFinal()
                    ? "This application is already " + before
                    : "Can't move from " + before + " to " + target + " (stages only move forward)");
        }
        recordHistory(application, before, userId);
        return CompanyApplicationResponse.from(application, timelineOf(applicationId), notesOf(applicationId));
    }

    // ---- private company data: notes + rating (never shown to the seeker, never a "reply") ----

    /** Add a private note to an application of MY company. */
    @Transactional
    public NoteResponse addNote(UUID userId, UUID applicationId, String text) {
        Application application = findCompanyApplication(userId, applicationId);
        ApplicationNote note = new ApplicationNote(UUID.randomUUID(), application.getId(), userId, text.trim(),
                clock.instant());
        noteRepository.save(note);
        return NoteResponse.from(note, userService.emailsByIds(List.of(userId)).get(userId));
    }

    /** Delete MY OWN note. Not my company's application → 404; unknown note → 404; someone else's → 403. */
    @Transactional
    public void deleteNote(UUID userId, UUID applicationId, UUID noteId) {
        Application application = findCompanyApplication(userId, applicationId);
        ApplicationNote note = noteRepository.findByIdAndApplicationId(noteId, application.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.isWrittenBy(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the author can delete a note");
        }
        noteRepository.delete(note);
    }

    /** Set / change / clear (null) the company's 1–5 rating. Last change wins. Any status. */
    @Transactional
    public CompanyApplicationResponse rate(UUID userId, UUID applicationId, Integer rating) {
        Application application = findCompanyApplication(userId, applicationId);
        application.rate(rating);
        return CompanyApplicationResponse.from(application, timelineOf(applicationId), notesOf(applicationId));
    }

    /** The resume version SENT with this application (not the seeker's current one). */
    @Transactional(readOnly = true)
    public FileContent openResumeForCompany(UUID userId, UUID applicationId) {
        return fileService.open(findCompanyApplication(userId, applicationId).getResumeFileId());
    }

    // an application of MY company (no lock). Not ours / unknown / no company → 404.
    private Application findCompanyApplication(UUID userId, UUID applicationId) {
        return applicationRepository.findByIdAndCompanyId(applicationId, myCompanyId(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    // notes oldest first, with author emails from ONE batch lookup (deleted author → email null)
    private List<NoteResponse> notesOf(UUID applicationId) {
        List<ApplicationNote> notes = noteRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
        Set<UUID> authorIds = notes.stream().map(ApplicationNote::getAuthorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> emails = userService.emailsByIds(authorIds);
        return notes.stream()
                .map(n -> NoteResponse.from(n, n.getAuthorId() == null ? null : emails.get(n.getAuthorId())))
                .toList();   // (Map.of().get(null) would throw, hence the null check)
    }

    // company from the DB (token's companyId can be stale). No company → 404 (nothing of theirs exists).
    private UUID myCompanyId(UUID userId) {
        UUID companyId = userService.getCompanyId(userId);
        if (companyId == null) {
            throw new ApiException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return companyId;
    }

    // timeline = status history, oldest first
    private List<TimelineItem> timelineOf(UUID applicationId) {
        return historyRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(h -> new TimelineItem(h.getToStatus(), h.getCreatedAt()))
                .toList();
    }

    // add one row to the audit trail: from → the application's NEW status, by whom (null = system)
    private void recordHistory(Application application, ApplicationStatus from, UUID changedBy) {
        historyRepository.save(new ApplicationStatusHistory(UUID.randomUUID(), application.getId(),
                from, application.getStatus(), changedBy, application.getUpdatedAt()));
    }

    private JobSummaryResponse card(UUID jobId) {
        return jobSearchService.cardsByIds(List.of(jobId)).get(jobId);
    }
}
