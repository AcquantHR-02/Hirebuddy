package com.jobportal.kaamsathi.job.service;

import com.jobportal.kaamsathi.common.dto.NamedRef;
import com.jobportal.kaamsathi.common.dto.PageResponse;
import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.common.util.Paging;
import com.jobportal.kaamsathi.company.service.CompanyService;
import com.jobportal.kaamsathi.job.dto.JobDetailResponse;
import com.jobportal.kaamsathi.job.dto.JobSearchFilter;
import com.jobportal.kaamsathi.job.dto.JobSummaryResponse;
import com.jobportal.kaamsathi.job.entity.Job;
import com.jobportal.kaamsathi.job.entity.JobStatus;
import com.jobportal.kaamsathi.job.repository.JobRepository;
import com.jobportal.kaamsathi.masterdata.service.CityService;
import com.jobportal.kaamsathi.masterdata.service.JobCategoryService;
import com.jobportal.kaamsathi.masterdata.service.SkillService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public side of jobs: search + job page. Only live jobs (ACTIVE, not past expiry) are ever shown.
 * Separate from JobService (company side) so each class has one job.
 */
@Service
public class JobSearchService {

    static final int MIN_WORD_LENGTH = 3;       // MySQL InnoDB FULLTEXT ignores shorter words (innodb_ft_min_token_size)

    private final JobRepository jobRepository;
    private final CompanyService companyService;
    private final JobCategoryService categoryService;
    private final CityService cityService;
    private final SkillService skillService;
    private final Clock clock;

    public JobSearchService(JobRepository jobRepository, CompanyService companyService,
                            JobCategoryService categoryService, CityService cityService,
                            SkillService skillService, Clock clock) {
        this.jobRepository = jobRepository;
        this.companyService = companyService;
        this.categoryService = categoryService;
        this.cityService = cityService;
        this.skillService = skillService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummaryResponse> search(JobSearchFilter f) {
        Page<Job> jobs = jobRepository.search(
                toBooleanQuery(f.q()),
                str(f.cityId()), str(f.categoryId()), str(f.skillId()),
                f.workMode() == null ? null : f.workMode().name(),
                f.jobType() == null ? null : f.jobType().name(),
                f.experience(), f.salaryMin(),
                clock.instant(),
                Paging.of(f.page(), f.size()));        // no Sort here: ORDER BY is in the SQL

        Names names = loadNames(jobs.getContent());
        return PageResponse.from(jobs, job -> toSummary(job, names));
    }

    // public job page. Closed / expired / blocked / unknown → 404 (same answer for all).
    @Transactional(readOnly = true)
    public JobDetailResponse getPublic(UUID jobId) {
        Instant now = clock.instant();
        Job job = jobRepository.findByIdAndStatusAndExpiresAtAfter(jobId, JobStatus.ACTIVE, now)
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_NOT_FOUND));
        return new JobDetailResponse(toSummary(job, loadNames(List.of(job))), job.getDescription());
    }

    // cards for any jobs (also closed ones), names loaded in one go. Used by SavedJobService (same package).
    /** For other modules (application): job cards by id, ANY status (an applied job may be closed now). */
    @Transactional(readOnly = true)
    public Map<UUID, JobSummaryResponse> cardsByIds(Collection<UUID> jobIds) {
        return toSummaries(jobRepository.findAllById(jobIds));
    }

    Map<UUID, JobSummaryResponse> toSummaries(List<Job> jobs) {
        Names names = loadNames(jobs);
        return jobs.stream().collect(Collectors.toMap(Job::getId, j -> toSummary(j, names)));
    }

    // ---- helpers ----

    /*
     * User text → MySQL BOOLEAN MODE query. "Java  devel" → "java* devel*"
     * - keep only letters/digits (removes + - * " ( ) ~ < > @ which are BOOLEAN MODE operators → no injection of syntax)
     * - drop words shorter than 3 (FULLTEXT ignores them anyway)
     * - word* = prefix match ("dev" finds "developer"); no "+" → ANY word may match, best matches rank first
     * Nothing usable left → "" = no keyword filter.
     */
    static String toBooleanQuery(String q) {
        if (q == null) {
            return "";
        }
        return Arrays.stream(q.toLowerCase().split("[^\\p{L}\\p{N}]+"))
                .filter(w -> w.length() >= MIN_WORD_LENGTH)
                .distinct()
                .limit(10)                                 // a search box doesn't need more; keeps the query cheap
                .map(w -> w + "*")
                .collect(Collectors.joining(" "));
    }

    // names for ALL jobs of the page in 4 queries (not 4 per job)
    private Names loadNames(List<Job> jobs) {
        Set<UUID> companyIds = jobs.stream().map(Job::getCompanyId).collect(Collectors.toSet());
        Set<UUID> categoryIds = jobs.stream().map(Job::getCategoryId).collect(Collectors.toSet());
        Set<UUID> cityIds = jobs.stream().map(Job::getCityId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> skillIds = jobs.stream().flatMap(j -> j.getSkillIds().stream()).collect(Collectors.toSet());
        return new Names(
                companyService.namesByIds(companyIds),
                categoryService.namesByIds(categoryIds),
                cityService.labelsByIds(cityIds),
                skillService.namesByIds(skillIds));
    }

    private static JobSummaryResponse toSummary(Job j, Names n) {
        List<NamedRef> skills = j.getSkillIds().stream()
                .map(id -> new NamedRef(id, n.skills().get(id)))
                .sorted(Comparator.comparing(NamedRef::name, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return new JobSummaryResponse(
                j.getId(),
                j.getTitle(),
                new NamedRef(j.getCompanyId(), n.companies().get(j.getCompanyId())),
                new NamedRef(j.getCategoryId(), n.categories().get(j.getCategoryId())),
                j.getCityId() == null ? null : new NamedRef(j.getCityId(), n.cities().get(j.getCityId())),
                j.getWorkMode(),
                j.getJobType(),
                j.getExperienceMin(),
                j.getExperienceMax() == null ? null : j.getExperienceMax().intValue(),
                j.getSalaryMin(),
                j.getSalaryMax(),
                skills,
                j.getCreatedAt(),
                j.getExpiresAt());
    }

    private static String str(UUID id) {
        return id == null ? null : id.toString();
    }

    // the 4 id → name maps for one page
    private record Names(Map<UUID, String> companies, Map<UUID, String> categories,
                         Map<UUID, String> cities, Map<UUID, String> skills) {
    }
}
