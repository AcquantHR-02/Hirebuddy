-- V19: subscriptions = the plan a company REALLY has, made when the admin approves a payment request.
-- Limits are COPIED from the plan at approval time (admin editing the plan later never changes a bought subscription).
-- Renewal approved early → the new row starts when the current one ends (status PENDING until then).
-- Until the Phase 5 scheduler updates `status`, the code decides "active now" from the DATES
-- (starts_at <= now < ends_at, + 3-day grace for posting) and ignores CANCELLED rows.
-- NEVER edit this file after it has run. Add V20__... for changes.

CREATE TABLE subscriptions (
    id                 CHAR(36)    NOT NULL,            -- UUID, made by our Java code
    company_id         CHAR(36)    NOT NULL,            -- who owns this plan period
    plan_id            CHAR(36)    NOT NULL,            -- which plan was bought (for the name / history)
    payment_request_id CHAR(36)    NOT NULL,            -- the approved payment that created it
    job_slots          INT         NOT NULL,            -- COPY: max ACTIVE jobs at the same time
    recruiter_seats    INT         NOT NULL,            -- COPY: max recruiters (active + pending invites)
    starts_at          DATETIME(6) NOT NULL,            -- UTC
    ends_at            DATETIME(6) NOT NULL,            -- UTC (starts_at + plan duration)
    status             VARCHAR(20) NOT NULL,            -- PENDING → ACTIVE → EXPIRED / CANCELLED
    created_at         DATETIME(6) NOT NULL,            -- UTC
    updated_at         DATETIME(6) NOT NULL,            -- UTC

    CONSTRAINT pk_subscriptions          PRIMARY KEY (id),
    CONSTRAINT uk_subscriptions_payment  UNIQUE (payment_request_id),     -- one payment → at most one subscription
    CONSTRAINT ck_subscriptions_status   CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_subscriptions_period   CHECK (ends_at > starts_at),
    CONSTRAINT ck_subscriptions_slots    CHECK (job_slots >= 1),
    CONSTRAINT ck_subscriptions_seats    CHECK (recruiter_seats >= 1),
    CONSTRAINT fk_subscriptions_company  FOREIGN KEY (company_id)         REFERENCES companies (id),
    CONSTRAINT fk_subscriptions_plan     FOREIGN KEY (plan_id)            REFERENCES plans (id),
    CONSTRAINT fk_subscriptions_payment  FOREIGN KEY (payment_request_id) REFERENCES payment_requests (id)
);

-- "what does this company have right now / when does its latest period end?"
CREATE INDEX idx_subscriptions_company_ends ON subscriptions (company_id, ends_at);
-- Phase 5 scheduler: rows to switch PENDING → ACTIVE → EXPIRED
CREATE INDEX idx_subscriptions_status_ends  ON subscriptions (status, ends_at);
