package com.jobportal.kaamsathi.job.repository;

import com.jobportal.kaamsathi.job.entity.SavedJob;
import com.jobportal.kaamsathi.job.entity.SavedJobId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * DB access for `saved_jobs`. Only the job module uses this.
 */
public interface SavedJobRepository extends JpaRepository<SavedJob, SavedJobId> {

    // SELECT * FROM saved_jobs WHERE seeker_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?
    // "IdSeekerId" = the seekerId field INSIDE the id
    Page<SavedJob> findByIdSeekerIdOrderByCreatedAtDesc(UUID seekerId, Pageable pageable);
}
