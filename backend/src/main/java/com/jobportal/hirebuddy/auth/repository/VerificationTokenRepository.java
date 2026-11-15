package com.jobportal.hirebuddy.auth.repository;

import com.jobportal.hirebuddy.auth.entity.TokenType;
import com.jobportal.hirebuddy.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * DB access for the `verification_tokens` table (auth module only).
 */
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    // SELECT * FROM verification_tokens WHERE token_hash = ? AND type = ?
    // type too: a PASSWORD_RESET token must never work as EMAIL_VERIFY (and the other way round)
    Optional<VerificationToken> findByTokenHashAndType(String tokenHash, TokenType type);
}
