-- V10: a seeker's saved jobs ("bookmarks"). One row per (seeker, job) pair.
-- NEVER edit this file after it has run. Add V11__... for changes.

CREATE TABLE saved_jobs (
    seeker_id   CHAR(36)    NOT NULL,   -- the SEEKER user
    job_id      CHAR(36)    NOT NULL,
    created_at  DATETIME(6) NOT NULL,   -- UTC; when it was saved (list is newest first)

    CONSTRAINT pk_saved_jobs        PRIMARY KEY (seeker_id, job_id),   -- same job saved only once
    CONSTRAINT fk_saved_jobs_seeker FOREIGN KEY (seeker_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_jobs_job    FOREIGN KEY (job_id)    REFERENCES jobs (id)  ON DELETE CASCADE
);

-- "my saved jobs, newest first"
CREATE INDEX idx_saved_jobs_seeker_created ON saved_jobs (seeker_id, created_at);
