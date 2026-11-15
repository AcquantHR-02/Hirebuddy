package com.jobportal.hirebuddy.auth.repository;

import com.jobportal.hirebuddy.auth.entity.RecruiterInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * DB access for the `recruiter_invites` table (auth module only).
 */
public interface RecruiterInviteRepository extends JpaRepository<RecruiterInvite, UUID> {

    // SELECT * FROM recruiter_invites WHERE token_hash = ?   (UNIQUE index → fast)
    Optional<RecruiterInvite> findByTokenHash(String tokenHash);
}
