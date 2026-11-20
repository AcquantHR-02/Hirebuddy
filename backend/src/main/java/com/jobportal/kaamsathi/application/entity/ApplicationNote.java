package com.jobportal.kaamsathi.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One private note on an application (V16), written by the owner / a recruiter. Never shown to the seeker.
 * Immutable: notes are added or deleted, never edited.
 */
@Entity
@Table(name = "application_notes")
public class ApplicationNote {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "application_id", nullable = false, length = 36)
    private UUID applicationId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "author_id", length = 36)
    private UUID authorId;            // null = author's account was deleted

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApplicationNote() {
    }

    public ApplicationNote(UUID id, UUID applicationId, UUID authorId, String body, Instant now) {
        this.id = id;
        this.applicationId = applicationId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = now;
    }

    // only the author may delete it
    public boolean isWrittenBy(UUID userId) {
        return authorId != null && authorId.equals(userId);
    }

    public UUID getId() { return id; }
    public UUID getApplicationId() { return applicationId; }
    public UUID getAuthorId() { return authorId; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
