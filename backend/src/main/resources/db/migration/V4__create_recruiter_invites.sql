-- V4: owner invites a recruiter by email. Like other tokens, we store only the SHA-256 hash.
-- NEVER edit this file after it has run. Add V5__... for changes.

CREATE TABLE recruiter_invites (
    id           CHAR(36)     NOT NULL,                -- UUID, made by our Java code
    email        VARCHAR(254) NOT NULL,                -- who is invited (lowercase)
    company_id   CHAR(36)     NOT NULL,                -- company they will join (FK to companies comes in Phase 2)
    invited_by   CHAR(36)     NOT NULL,                -- owner user id
    token_hash   CHAR(64)     NOT NULL,                -- SHA-256 of the token as hex
    status       VARCHAR(20)  NOT NULL,                -- PENDING / ACCEPTED
    expires_at   DATETIME(6)  NOT NULL,                -- UTC
    accepted_at  DATETIME(6)  NULL,                    -- NULL = not accepted yet; single-use
    created_at   DATETIME(6)  NOT NULL,                -- UTC

    CONSTRAINT pk_recruiter_invites        PRIMARY KEY (id),
    CONSTRAINT uk_recruiter_invites_hash   UNIQUE (token_hash),
    CONSTRAINT ck_recruiter_invites_status CHECK (status IN ('PENDING', 'ACCEPTED')),
    CONSTRAINT fk_recruiter_invites_owner  FOREIGN KEY (invited_by)
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX ix_recruiter_invites_company (company_id)   -- "all invites of my company" later
);
