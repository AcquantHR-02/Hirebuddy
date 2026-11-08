-- V2: refresh tokens (one row = one login session on one device).
-- We store only the SHA-256 hash of the token, never the token itself.
-- NEVER edit this file after it has run. Add V3__... for changes.

CREATE TABLE refresh_tokens (
    id          CHAR(36)    NOT NULL,                 -- UUID, made by our Java code
    user_id     CHAR(36)    NOT NULL,                 -- whose token
    token_hash  CHAR(64)    NOT NULL,                 -- SHA-256 of the token as hex (always 64 chars)
    expires_at  DATETIME(6) NOT NULL,                 -- created + 7 days (UTC)
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,   -- TRUE after refresh (rotation) or logout
    created_at  DATETIME(6) NOT NULL,                 -- UTC

    CONSTRAINT pk_refresh_tokens       PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash  UNIQUE (token_hash),       -- lookup by hash on every refresh
    CONSTRAINT fk_refresh_tokens_user  FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE                   -- account deleted -> its tokens go too
);
