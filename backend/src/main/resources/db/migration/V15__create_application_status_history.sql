-- V15: every status change of an application (who, what, when).
-- Gives the seeker a timeline (Applied → Viewed → Shortlisted …) and later feeds notifications + the employer score.
-- Rows are only ever ADDED, never changed (an audit trail).
-- NEVER edit this file after it has run. Add V16__... for changes.

CREATE TABLE application_status_history (
    id              CHAR(36)    NOT NULL,   -- UUID
    application_id  CHAR(36)    NOT NULL,
    from_status     VARCHAR(20) NULL,       -- NULL for the first row (APPLIED)
    to_status       VARCHAR(20) NOT NULL,
    changed_by      CHAR(36)    NULL,       -- user who did it; NULL = system (auto-closure) or user deleted
    created_at      DATETIME(6) NOT NULL,   -- UTC

    CONSTRAINT pk_application_status_history PRIMARY KEY (id),
    CONSTRAINT ck_ash_to_status CHECK (to_status IN ('APPLIED', 'VIEWED', 'SHORTLISTED', 'INTERVIEW', 'OFFERED',
                                                     'HIRED', 'REJECTED', 'WITHDRAWN', 'CLOSED_NO_RESPONSE')),
    CONSTRAINT fk_ash_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_ash_changed_by  FOREIGN KEY (changed_by)     REFERENCES users (id)        ON DELETE SET NULL
);

-- "timeline of one application, oldest first"
CREATE INDEX idx_ash_application_created ON application_status_history (application_id, created_at);
