-- V1: the users table (one row = one account: seeker, company owner, recruiter or admin).
-- NEVER edit this file after it has run. To change the table, add V2__..., V3__... instead.

CREATE TABLE users (
    id                  CHAR(36)     NOT NULL,                 -- UUID, made by our Java code
    email               VARCHAR(254) NOT NULL,                 -- login name; 254 = max email length
    password_hash       VARCHAR(100) NOT NULL,                 -- BCrypt hash, never the real password
    role                VARCHAR(20)  NOT NULL,                 -- SEEKER / COMPANY_OWNER / RECRUITER / ADMIN
    status              VARCHAR(20)  NOT NULL,                 -- UNVERIFIED / ACTIVE / SUSPENDED
    company_id          CHAR(36)     NULL,                     -- only for owners/recruiters (company lives in core_db)
    failed_login_count  INT          NOT NULL DEFAULT 0,       -- wrong passwords in a row
    locked_until        DATETIME(6)  NULL,                     -- login blocked until this time (UTC)
    created_at          DATETIME(6)  NOT NULL,                 -- UTC
    updated_at          DATETIME(6)  NOT NULL,                 -- UTC

    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT uk_users_email       UNIQUE (email),
    CONSTRAINT ck_users_role        CHECK (role IN ('SEEKER', 'COMPANY_OWNER', 'RECRUITER', 'ADMIN')),
    CONSTRAINT ck_users_status      CHECK (status IN ('UNVERIFIED', 'ACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_users_failed_cnt  CHECK (failed_login_count >= 0)
);

-- Fast lookup of "all users of company X" (team page, recruiter list)
CREATE INDEX idx_users_company_id ON users (company_id);
