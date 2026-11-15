-- V3: one-time tokens sent by email: verify email address, reset password.
-- Like refresh tokens, we store only the SHA-256 hash, never the token itself.
-- NEVER edit this file after it has run. Add V4__... for changes.

CREATE TABLE verification_tokens (
    id          CHAR(36)    NOT NULL,                 -- UUID, made by our Java code
    user_id     CHAR(36)    NOT NULL,                 -- whose token
    token_hash  CHAR(64)    NOT NULL,                 -- SHA-256 of the token as hex
    type        VARCHAR(20) NOT NULL,                 -- EMAIL_VERIFY / PASSWORD_RESET
    expires_at  DATETIME(6) NOT NULL,                 -- UTC
    used_at     DATETIME(6) NULL,                     -- NULL = not used yet; single-use
    created_at  DATETIME(6) NOT NULL,                 -- UTC

    CONSTRAINT pk_verification_tokens       PRIMARY KEY (id),
    CONSTRAINT uk_verification_tokens_hash  UNIQUE (token_hash),
    CONSTRAINT ck_verification_tokens_type  CHECK (type IN ('EMAIL_VERIFY', 'PASSWORD_RESET')),
    CONSTRAINT fk_verification_tokens_user  FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);
