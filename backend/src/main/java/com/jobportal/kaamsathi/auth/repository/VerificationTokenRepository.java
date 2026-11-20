package com.jobportal.kaamsathi.auth.repository;

import com.jobportal.kaamsathi.auth.entity.TokenType;
import com.jobportal.kaamsathi.auth.entity.VerificationToken;
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

    // the NEWEST code of this user + type (OTP: only the latest code counts; older ones are ignored)
    // SELECT * ... WHERE user_id = ? AND type = ? ORDER BY created_at DESC LIMIT 1
    Optional<VerificationToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, TokenType type);
}
