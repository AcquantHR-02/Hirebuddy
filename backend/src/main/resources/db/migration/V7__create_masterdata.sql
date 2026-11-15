-- V7: masterdata = fixed lists the admin manages; everyone else only picks from them.
-- active = FALSE hides an item from dropdowns but keeps old jobs/profiles that use it valid (no delete).
-- Names are UNIQUE. Our DB collation (utf8mb4_0900_ai_ci) is case-insensitive, so "Java" and "java" clash.
-- NEVER edit this file after it has run. Add V8__... for changes.

CREATE TABLE skills (
    id          CHAR(36)     NOT NULL,              -- UUID, made by our Java code
    name        VARCHAR(100) NOT NULL,              -- e.g. "Java", "React"
    active      BOOLEAN      NOT NULL DEFAULT TRUE, -- FALSE = hidden from dropdowns
    created_at  DATETIME(6)  NOT NULL,              -- UTC
    updated_at  DATETIME(6)  NOT NULL,              -- UTC

    CONSTRAINT pk_skills      PRIMARY KEY (id),
    CONSTRAINT uk_skills_name UNIQUE (name)
);

CREATE TABLE industries (
    id          CHAR(36)     NOT NULL,
    name        VARCHAR(100) NOT NULL,              -- e.g. "IT Services", "Banking"
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    CONSTRAINT pk_industries      PRIMARY KEY (id),
    CONSTRAINT uk_industries_name UNIQUE (name)
);

CREATE TABLE job_categories (
    id          CHAR(36)     NOT NULL,
    name        VARCHAR(100) NOT NULL,              -- e.g. "Software Development", "Sales"
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    CONSTRAINT pk_job_categories      PRIMARY KEY (id),
    CONSTRAINT uk_job_categories_name UNIQUE (name)
);

CREATE TABLE cities (
    id          CHAR(36)     NOT NULL,
    name        VARCHAR(100) NOT NULL,              -- e.g. "Aurangabad"
    state       VARCHAR(100) NOT NULL,              -- e.g. "Maharashtra" (same city name exists in 2 states)
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    CONSTRAINT pk_cities            PRIMARY KEY (id),
    CONSTRAINT uk_cities_name_state UNIQUE (name, state)   -- unique TOGETHER, not each alone
);
