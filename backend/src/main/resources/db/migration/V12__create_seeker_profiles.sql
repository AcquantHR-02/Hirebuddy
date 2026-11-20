-- V12: seeker profile = one row per SEEKER user (+ small child tables for lists).
-- The profile row is made on the first save (PUT /api/seekers/me), not at signup.
-- NEVER edit this file after it has run. Add V13__... for changes.

CREATE TABLE seeker_profiles (
    user_id           CHAR(36)     NOT NULL,          -- same id as users.id (one profile per user)
    full_name         VARCHAR(100) NOT NULL,
    phone             VARCHAR(15)  NULL,              -- Indian mobile, 10 digits
    headline          VARCHAR(150) NULL,              -- e.g. "Java developer, 3 years"
    about             TEXT         NULL,
    city_id           CHAR(36)     NULL,              -- current city (masterdata)
    experience_years  SMALLINT     NOT NULL,          -- total years
    expected_salary   INT          NULL,              -- rupees per year
    resume_file_id    CHAR(36)     NULL,              -- current resume (files); set in Part 3
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,

    CONSTRAINT pk_seeker_profiles        PRIMARY KEY (user_id),
    CONSTRAINT ck_seeker_profiles_exp    CHECK (experience_years >= 0),
    CONSTRAINT ck_seeker_profiles_salary CHECK (expected_salary >= 0),
    CONSTRAINT fk_seeker_profiles_user   FOREIGN KEY (user_id)        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_seeker_profiles_city   FOREIGN KEY (city_id)        REFERENCES cities (id),
    CONSTRAINT fk_seeker_profiles_resume FOREIGN KEY (resume_file_id) REFERENCES files (id) ON DELETE SET NULL
);

-- skills the seeker has (many-to-many, like job_skills)
CREATE TABLE seeker_skills (
    user_id   CHAR(36) NOT NULL,
    skill_id  CHAR(36) NOT NULL,
    CONSTRAINT pk_seeker_skills       PRIMARY KEY (user_id, skill_id),
    CONSTRAINT fk_seeker_skills_user  FOREIGN KEY (user_id)  REFERENCES seeker_profiles (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_seeker_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id)
);

-- cities where the seeker wants to work
CREATE TABLE seeker_preferred_cities (
    user_id  CHAR(36) NOT NULL,
    city_id  CHAR(36) NOT NULL,
    CONSTRAINT pk_seeker_preferred_cities      PRIMARY KEY (user_id, city_id),
    CONSTRAINT fk_seeker_preferred_cities_user FOREIGN KEY (user_id) REFERENCES seeker_profiles (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_seeker_preferred_cities_city FOREIGN KEY (city_id) REFERENCES cities (id)
);

-- education list. position = order on screen (0, 1, 2…). Saved as a whole list (full replace).
CREATE TABLE seeker_education (
    user_id         CHAR(36)     NOT NULL,
    position        INT          NOT NULL,
    degree          VARCHAR(100) NOT NULL,          -- e.g. "B.Tech"
    institute       VARCHAR(150) NOT NULL,
    field_of_study  VARCHAR(100) NULL,              -- e.g. "Computer Science"
    start_year      SMALLINT     NOT NULL,
    end_year        SMALLINT     NULL,              -- NULL = still studying
    CONSTRAINT pk_seeker_education      PRIMARY KEY (user_id, position),
    CONSTRAINT fk_seeker_education_user FOREIGN KEY (user_id) REFERENCES seeker_profiles (user_id) ON DELETE CASCADE
);

-- work experience list (same idea)
CREATE TABLE seeker_experience (
    user_id       CHAR(36)      NOT NULL,
    position      INT           NOT NULL,
    job_title     VARCHAR(100)  NOT NULL,
    company_name  VARCHAR(150)  NOT NULL,
    start_date    DATE          NOT NULL,
    end_date      DATE          NULL,               -- NULL = current job
    description   VARCHAR(2000) NULL,
    CONSTRAINT pk_seeker_experience      PRIMARY KEY (user_id, position),
    CONSTRAINT fk_seeker_experience_user FOREIGN KEY (user_id) REFERENCES seeker_profiles (user_id) ON DELETE CASCADE
);
