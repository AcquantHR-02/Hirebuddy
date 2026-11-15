-- V5: companies. An owner registers ONE company; it starts PENDING until the admin verifies it.
-- NEVER edit this file after it has run. Add V6__... for changes.

CREATE TABLE companies (
    id                CHAR(36)     NOT NULL,              -- UUID, made by our Java code
    owner_id          CHAR(36)     NOT NULL,              -- the COMPANY_OWNER user
    name              VARCHAR(150) NOT NULL,
    gstin             VARCHAR(15)  NULL,                  -- Indian GST number (15 chars), optional
    website           VARCHAR(255) NULL,
    industry          VARCHAR(100) NULL,                  -- plain text for now (masterdata later)
    size              VARCHAR(20)  NULL,                  -- e.g. "11-50"
    location          VARCHAR(150) NULL,                  -- plain text for now (masterdata later)
    status            VARCHAR(20)  NOT NULL,              -- PENDING / VERIFIED / REJECTED
    rejection_reason  VARCHAR(500) NULL,                  -- only when REJECTED
    created_at        DATETIME(6)  NOT NULL,              -- UTC
    updated_at        DATETIME(6)  NOT NULL,              -- UTC

    CONSTRAINT pk_companies        PRIMARY KEY (id),
    CONSTRAINT uk_companies_owner  UNIQUE (owner_id),     -- one company per owner
    CONSTRAINT uk_companies_gstin  UNIQUE (gstin),        -- many NULLs are allowed by MySQL
    CONSTRAINT ck_companies_status CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    CONSTRAINT fk_companies_owner  FOREIGN KEY (owner_id) REFERENCES users (id)
);

-- Admin review list: "all PENDING companies"
CREATE INDEX idx_companies_status ON companies (status);
