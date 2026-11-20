-- V14: job applications. One row = one seeker applied to one job.
-- profile_snapshot + resume_file_id are FROZEN at apply time: later profile / resume edits don't change them.
-- NEVER edit this file after it has run. Add V15__... for changes.

CREATE TABLE applications (
    id                 CHAR(36)    NOT NULL,   -- UUID
    job_id             CHAR(36)    NOT NULL,
    company_id         CHAR(36)    NOT NULL,   -- copied from the job: recruiter lists + employer score without a join
    seeker_id          CHAR(36)    NOT NULL,   -- the SEEKER user
    status             VARCHAR(20) NOT NULL,
    resume_file_id     CHAR(36)    NOT NULL,   -- the resume version sent (old versions are never deleted)
    profile_snapshot   JSON        NOT NULL,   -- copy of the profile at apply time (names filled in)
    viewed_at          DATETIME(6) NULL,       -- UTC; first time a recruiter opened it (tracker "Viewed")
    first_response_at  DATETIME(6) NULL,       -- UTC; first move past VIEWED, reject included (employer score)
    created_at         DATETIME(6) NOT NULL,   -- UTC; applied at
    updated_at         DATETIME(6) NOT NULL,   -- UTC

    CONSTRAINT pk_applications            PRIMARY KEY (id),
    CONSTRAINT uk_applications_job_seeker UNIQUE (job_id, seeker_id),          -- one application per job, ever
    CONSTRAINT ck_applications_status     CHECK (status IN ('APPLIED', 'VIEWED', 'SHORTLISTED', 'INTERVIEW',
                                                            'OFFERED', 'HIRED', 'REJECTED', 'WITHDRAWN',
                                                            'CLOSED_NO_RESPONSE')),
    CONSTRAINT fk_applications_job        FOREIGN KEY (job_id)         REFERENCES jobs (id),
    CONSTRAINT fk_applications_company    FOREIGN KEY (company_id)     REFERENCES companies (id),
    CONSTRAINT fk_applications_seeker     FOREIGN KEY (seeker_id)      REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_applications_resume     FOREIGN KEY (resume_file_id) REFERENCES files (id) ON DELETE CASCADE
);

-- "my applications, newest first" (seeker)
CREATE INDEX idx_applications_seeker_created  ON applications (seeker_id, created_at);
-- daily job: "still APPLIED / VIEWED and older than 14 days"
CREATE INDEX idx_applications_status_created  ON applications (status, created_at);
-- recruiter lists + employer score (last 90 days of one company)
CREATE INDEX idx_applications_company_created ON applications (company_id, created_at);
