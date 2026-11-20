package com.jobportal.kaamsathi.application.repository;

import com.jobportal.kaamsathi.application.entity.Application;
import com.jobportal.kaamsathi.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Only the application module touches the `applications` table.
 */
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    // already applied? (fast path; the UNIQUE key is the real guard)
    boolean existsByJobIdAndSeekerId(UUID jobId, UUID seekerId);

    // "my applications", newest first (index seeker_id, created_at)
    Page<Application> findBySeekerIdOrderByCreatedAtDesc(UUID seekerId, Pageable pageable);

    // the seeker's OWN application (someone else's → empty → 404)
    Optional<Application> findByIdAndSeekerId(UUID id, UUID seekerId);

    // applicants of one job, newest first; status null = all statuses (index job_id via UNIQUE (job_id, seeker_id))
    @Query("""
            SELECT a FROM Application a
            WHERE a.jobId = :jobId AND (:status IS NULL OR a.status = :status)
            ORDER BY a.createdAt DESC""")
    Page<Application> findForJob(UUID jobId, ApplicationStatus status, Pageable pageable);

    // an application of MY company (another company's → empty → 404)
    Optional<Application> findByIdAndCompanyId(UUID id, UUID companyId);

    // Same lookups, but LOCK the row (SELECT … FOR UPDATE) until the transaction ends.
    // Used before a status change: recruiter move + seeker withdraw at the same moment → the 2nd waits,
    // then sees the real status (no lost update). "ForUpdate" in the name is only a label for Spring Data.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Application> findForUpdateByIdAndCompanyId(UUID id, UUID companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Application> findForUpdateByIdAndSeekerId(UUID id, UUID seekerId);

    /**
     * First open by the company: APPLIED → VIEWED in ONE atomic UPDATE.
     * Returns 1 only for the very first open; 0 if already viewed / moved / withdrawn / not ours.
     * Two recruiters at the same moment → the DB lets only one of them change the row.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Application a
            SET a.status = com.jobportal.kaamsathi.application.entity.ApplicationStatus.VIEWED,
                a.viewedAt = :now, a.updatedAt = :now
            WHERE a.id = :id AND a.companyId = :companyId
              AND a.status = com.jobportal.kaamsathi.application.entity.ApplicationStatus.APPLIED""")
    int markViewed(UUID id, UUID companyId, Instant now);
}
