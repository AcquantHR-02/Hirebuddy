package com.jobportal.kaamsathi.application.dto;

import com.jobportal.kaamsathi.application.entity.ApplicationNote;

import java.time.Instant;
import java.util.UUID;

/**
 * One private note. authorEmail = who wrote it (null if that account was deleted).
 */
public record NoteResponse(
        UUID id,
        UUID authorId,
        String authorEmail,
        String text,
        Instant createdAt
) {

    public static NoteResponse from(ApplicationNote note, String authorEmail) {
        return new NoteResponse(note.getId(), note.getAuthorId(), authorEmail, note.getBody(), note.getCreatedAt());
    }
}
