-- V8: jobs. A job belongs to a COMPANY (not to the recruiter who wrote it).
-- Skills are many-to-many -> separate table job_skills.
-- NEVER edit this file after it has run. Add V9__... for changes.

CREATE TABLE jobs (
    id              CHAR(36)     NOT NULL,              -- UUID, made by our Java code
    company_id      CHAR(36)     NOT NULL,              -- owner of the job
    created_by      CHAR(36)     NULL,                  -- user who posted it; NULL if that user was deleted
    title           VARCHAR(150) NOT NULL,
    description     TEXT         NOT NULL,              -- long text (up to 64 KB)
    category_id     CHAR(36)     NOT NULL,              -- masterdata job_categories
    city_id         CHAR(36)     NULL,                  -- masterdata cities; NULL only for REMOTE jobs
    work_mode       VARCHAR(20)  NOT NULL,              -- REMOTE / HYBRID / ONSITE
    job_type        VARCHAR(20)  NOT NULL,              -- FULL_TIME / PART_TIME / INTERNSHIP / CONTRACT
    experience_min  SMALLINT     NOT NULL,              -- years
    experience_max  SMALLINT     NULL,                  -- years; NULL = no upper limit
    salary_min      INT          NULL,                  -- rupees per year; NULL = not shown
    salary_max      INT          NULL,
    status          VARCHAR(20)  NOT NULL,              -- DRAFT / ACTIVE / CLOSED / EXPIRED / BLOCKED
    expires_at      DATETIME(6)  NOT NULL,              -- UTC; posted + 60 days
    closed_at       DATETIME(6)  NULL,                  -- UTC; when CLOSED / EXPIRED
    created_at      DATETIME(6)  NOT NULL,              -- UTC
    updated_at      DATETIME(6)  NOT NULL,              -- UTC

    CONSTRAINT pk_jobs            PRIMARY KEY (id),
    CONSTRAINT ck_jobs_status     CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'EXPIRED', 'BLOCKED')),
    CONSTRAINT ck_jobs_work_mode  CHECK (work_mode IN ('REMOTE', 'HYBRID', 'ONSITE')),
    CONSTRAINT ck_jobs_job_type   CHECK (job_type IN ('FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'CONTRACT')),
    CONSTRAINT ck_jobs_experience CHECK (experience_min >= 0 AND experience_max >= experience_min),
    CONSTRAINT ck_jobs_salary     CHECK (salary_min >= 0 AND salary_max >= salary_min),
    CONSTRAINT fk_jobs_company    FOREIGN KEY (company_id)  REFERENCES companies (id),
    CONSTRAINT fk_jobs_created_by FOREIGN KEY (created_by)  REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_jobs_category   FOREIGN KEY (category_id) REFERENCES job_categories (id),
    CONSTRAINT fk_jobs_city       FOREIGN KEY (city_id)     REFERENCES cities (id)
);

-- Search: only ACTIVE jobs that are not expired yet
CREATE INDEX idx_jobs_status_expires ON jobs (status, expires_at);
-- Keyword search (MySQL FULLTEXT, words in title + description)
CREATE FULLTEXT INDEX ftx_jobs_title_description ON jobs (title, description);

CREATE TABLE job_skills (
    job_id    CHAR(36) NOT NULL,
    skill_id  CHAR(36) NOT NULL,

    CONSTRAINT pk_job_skills       PRIMARY KEY (job_id, skill_id),   -- same skill only once per job
    CONSTRAINT fk_job_skills_job   FOREIGN KEY (job_id)   REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id)
);
