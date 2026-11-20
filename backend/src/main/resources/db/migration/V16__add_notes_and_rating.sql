-- V16: private company-side data on an application (the seeker never sees it).
--   1. applications.rating: ONE shared 1–5 rating per application (NULL = not rated); latest change wins.
--   2. application_notes: comment log by the owner / recruiters (add + delete own, no edit).
-- NEVER edit this file after it has run. Add V17__... for changes.

ALTER TABLE applications
    ADD COLUMN rating TINYINT NULL AFTER first_response_at,
    ADD CONSTRAINT ck_applications_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5);

CREATE TABLE application_notes (
    id              CHAR(36)      NOT NULL,   -- UUID
    application_id  CHAR(36)      NOT NULL,
    author_id       CHAR(36)      NULL,       -- NULL = author's account was deleted (the note stays)
    body            VARCHAR(2000) NOT NULL,
    created_at      DATETIME(6)   NOT NULL,   -- UTC

    CONSTRAINT pk_application_notes PRIMARY KEY (id),
    CONSTRAINT fk_an_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_an_author      FOREIGN KEY (author_id)      REFERENCES users (id)        ON DELETE SET NULL
);

-- "notes of one application, oldest first"
CREATE INDEX idx_an_application_created ON application_notes (application_id, created_at);
