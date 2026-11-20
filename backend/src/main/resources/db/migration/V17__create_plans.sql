-- V17: plans = what a company can buy (admin creates them; prices/limits are not fixed in code).
-- Free tier is NOT a row here: no subscription = free rules (5 posts lifetime, 1 recruiter).
-- active = FALSE hides a plan from the shop; old payments/subscriptions that use it stay valid (no delete).
-- NEVER edit this file after it has run. Add V18__... for changes.

CREATE TABLE plans (
    id              CHAR(36)     NOT NULL,              -- UUID, made by our Java code
    name            VARCHAR(100) NOT NULL,              -- e.g. "Starter", "Pro"
    price_rupees    INT          NOT NULL,              -- whole rupees for one period (fake payment)
    duration_days   INT          NOT NULL,              -- e.g. 30, 90, 365
    job_slots       INT          NOT NULL,              -- max ACTIVE jobs at the same time
    recruiter_seats INT          NOT NULL,              -- max recruiters (active + pending invites)
    active          BOOLEAN      NOT NULL DEFAULT TRUE, -- FALSE = can't be bought anymore
    created_at      DATETIME(6)  NOT NULL,              -- UTC
    updated_at      DATETIME(6)  NOT NULL,              -- UTC

    CONSTRAINT pk_plans          PRIMARY KEY (id),
    CONSTRAINT uk_plans_name     UNIQUE (name),
    CONSTRAINT ck_plans_price    CHECK (price_rupees > 0),
    CONSTRAINT ck_plans_duration CHECK (duration_days BETWEEN 1 AND 3650),
    CONSTRAINT ck_plans_slots    CHECK (job_slots BETWEEN 1 AND 1000),
    CONSTRAINT ck_plans_seats    CHECK (recruiter_seats BETWEEN 1 AND 1000)
);
