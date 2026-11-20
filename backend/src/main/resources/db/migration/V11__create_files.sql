-- V11: uploaded files (profile pictures, resumes; later company logos, payment screenshots).
-- The BYTES live in file storage (local disk now, MinIO later); this table only keeps the facts about each file.
-- A file is never changed: "replace resume" = a NEW row, so old applications keep pointing at the old resume.
-- NEVER edit this file after it has run. Add V12__... for changes.

CREATE TABLE files (
    id             CHAR(36)     NOT NULL,              -- UUID
    owner_id       CHAR(36)     NOT NULL,              -- user who uploaded it
    kind           VARCHAR(20)  NOT NULL,              -- PHOTO / RESUME / LOGO / PAYMENT_PROOF
    original_name  VARCHAR(255) NOT NULL,              -- e.g. "Ravi_CV.pdf" (shown on download)
    content_type   VARCHAR(100) NOT NULL,              -- decided by US from the file's first bytes, not by the browser
    size_bytes     BIGINT       NOT NULL,
    storage_key    VARCHAR(255) NOT NULL,              -- where the bytes are, e.g. "resume/2b6f...". Never from user input.
    sha256         CHAR(64)     NOT NULL,              -- fingerprint of the content (spot duplicates / corruption)
    created_at     DATETIME(6)  NOT NULL,              -- UTC

    CONSTRAINT pk_files             PRIMARY KEY (id),
    CONSTRAINT uk_files_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_files_kind        CHECK (kind IN ('PHOTO', 'RESUME', 'LOGO', 'PAYMENT_PROOF')),
    CONSTRAINT ck_files_size        CHECK (size_bytes > 0),
    CONSTRAINT fk_files_owner       FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);
