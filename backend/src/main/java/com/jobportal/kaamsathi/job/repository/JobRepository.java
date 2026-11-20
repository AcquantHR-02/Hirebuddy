package com.jobportal.kaamsathi.job.repository;

import com.jobportal.kaamsathi.job.entity.Job;
import com.jobportal.kaamsathi.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DB access for `jobs` (+ `job_skills`). Only the job module uses this.
 */
public interface JobRepository extends JpaRepository<Job, UUID> {

    // SELECT COUNT(*) FROM jobs WHERE company_id = ?   (free quota: posts used, lifetime — closed jobs count too)
    long countByCompanyId(UUID companyId);

    // SELECT * FROM jobs WHERE id = ? AND company_id = ?   (a job of ANOTHER company is simply "not found")
    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    // SELECT * FROM jobs WHERE company_id = ? ORDER BY created_at DESC   (company's own list, newest first)
    List<Job> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    // public job page: only a live job (ACTIVE and not past expiry)
    Optional<Job> findByIdAndStatusAndExpiresAtAfter(UUID id, JobStatus status, Instant now);

    /*
     * Public search. NATIVE SQL because JPQL has no MATCH ... AGAINST (MySQL FULLTEXT).
     * Every filter has the form "(:x IS NULL OR ...)": a missing filter switches itself off.
     * ids / enums are passed as Strings (columns are CHAR / VARCHAR).
     * q = '' means "no keyword"; otherwise q is already in BOOLEAN MODE syntax (built in JobSearchService).
     * Sort: best keyword match first, then newest. Spring adds LIMIT/OFFSET from the Pageable.
     */
    String SEARCH_WHERE = """
            FROM jobs j
            WHERE j.status = 'ACTIVE'
              AND j.expires_at > :now
              AND (:q = '' OR MATCH(j.title, j.description) AGAINST (:q IN BOOLEAN MODE))
              AND (:cityId IS NULL OR j.city_id = :cityId)
              AND (:categoryId IS NULL OR j.category_id = :categoryId)
              AND (:workMode IS NULL OR j.work_mode = :workMode)
              AND (:jobType IS NULL OR j.job_type = :jobType)
              AND (:experience IS NULL OR (j.experience_min <= :experience
                   AND (j.experience_max IS NULL OR j.experience_max >= :experience)))
              AND (:salaryMin IS NULL OR j.salary_max >= :salaryMin
                   OR (j.salary_max IS NULL AND j.salary_min >= :salaryMin))
              AND (:skillId IS NULL OR EXISTS (
                   SELECT 1 FROM job_skills s WHERE s.job_id = j.id AND s.skill_id = :skillId))
            """;

    @Query(nativeQuery = true,
            value = "SELECT j.* " + SEARCH_WHERE + """
                    ORDER BY CASE WHEN :q = '' THEN 0
                                  ELSE MATCH(j.title, j.description) AGAINST (:q IN BOOLEAN MODE) END DESC,
                             j.created_at DESC
                    """,
            countQuery = "SELECT COUNT(*) " + SEARCH_WHERE)
    Page<Job> search(@Param("q") String q,
                     @Param("cityId") String cityId,
                     @Param("categoryId") String categoryId,
                     @Param("skillId") String skillId,
                     @Param("workMode") String workMode,
                     @Param("jobType") String jobType,
                     @Param("experience") Integer experience,
                     @Param("salaryMin") Integer salaryMin,
                     @Param("now") Instant now,
                     Pageable pageable);
}
