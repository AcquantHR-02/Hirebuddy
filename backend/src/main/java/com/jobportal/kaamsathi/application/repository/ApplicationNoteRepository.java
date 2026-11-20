package com.jobportal.kaamsathi.application.repository;

import com.jobportal.kaamsathi.application.entity.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Only the application module touches `application_notes`.
 */
public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, UUID> {

    // notes of one application, oldest first (index application_id, created_at)
    List<ApplicationNote> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    // the note must belong to THIS application (a note id from another application → empty → 404)
    Optional<ApplicationNote> findByIdAndApplicationId(UUID id, UUID applicationId);
}
