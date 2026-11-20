-- V18: payment_requests = a company says "I paid for this plan" (FAKE payment: no money moves).
-- The admin checks it and approves (→ subscription, V19) or rejects with a reason (owner may try again).
-- Rule: at most ONE pending request per company — guarded by the DB itself (see pending_company_id).
-- NEVER edit this file after it has run. Add V19__... for changes.

CREATE TABLE payment_requests (
    id                 CHAR(36)     NOT NULL,           -- UUID, made by our Java code
    company_id         CHAR(36)     NOT NULL,           -- who pays
    plan_id            CHAR(36)     NOT NULL,           -- what they buy
    amount_rupees      INT          NOT NULL,           -- COPY of the plan price at request time (admin may change the price later)
    transaction_id     VARCHAR(100) NOT NULL,           -- the (fake) UPI / bank reference the owner typed
    screenshot_file_id CHAR(36)     NULL,               -- optional proof image (files.kind = PAYMENT_PROOF)
    status             VARCHAR(20)  NOT NULL,           -- PENDING_APPROVAL → APPROVED / REJECTED
    rejection_reason   VARCHAR(500) NULL,               -- only when REJECTED
    requested_by       CHAR(36)     NULL,               -- the owner who sent it (NULL if that user is deleted later)
    reviewed_by        CHAR(36)     NULL,               -- the admin who approved / rejected
    reviewed_at        DATETIME(6)  NULL,               -- UTC
    created_at         DATETIME(6)  NOT NULL,           -- UTC
    updated_at         DATETIME(6)  NOT NULL,           -- UTC

    -- Filled by MySQL, never by Java: company_id while PENDING, else NULL.
    -- UNIQUE ignores NULLs → many APPROVED / REJECTED rows per company, but only ONE pending.
    pending_company_id CHAR(36) GENERATED ALWAYS AS
        (CASE WHEN status = 'PENDING_APPROVAL' THEN company_id END) STORED,

    CONSTRAINT pk_payment_requests            PRIMARY KEY (id),
    CONSTRAINT uk_payment_requests_pending    UNIQUE (pending_company_id),
    CONSTRAINT ck_payment_requests_status     CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_payment_requests_amount     CHECK (amount_rupees > 0),
    CONSTRAINT fk_payment_requests_company    FOREIGN KEY (company_id)         REFERENCES companies (id),
    CONSTRAINT fk_payment_requests_plan       FOREIGN KEY (plan_id)            REFERENCES plans (id),
    CONSTRAINT fk_payment_requests_screenshot FOREIGN KEY (screenshot_file_id) REFERENCES files (id) ON DELETE SET NULL,
    CONSTRAINT fk_payment_requests_requester  FOREIGN KEY (requested_by)       REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_payment_requests_reviewer   FOREIGN KEY (reviewed_by)        REFERENCES users (id) ON DELETE SET NULL
);

-- owner's billing history: their company's requests, newest first
CREATE INDEX idx_payment_requests_company_created ON payment_requests (company_id, created_at);
-- admin approval queue: pending requests, oldest first
CREATE INDEX idx_payment_requests_status_created  ON payment_requests (status, created_at);
