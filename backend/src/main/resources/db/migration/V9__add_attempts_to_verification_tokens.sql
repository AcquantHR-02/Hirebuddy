-- V9: email verification now uses a 6-digit OTP (typed on the register page) instead of a link.
-- A short code can be guessed, so we count wrong tries; after 5 the code is dead (user presses "Resend").
-- NEVER edit this file after it has run. Add V10__... for changes.

ALTER TABLE verification_tokens
    ADD COLUMN attempts INT NOT NULL DEFAULT 0 AFTER used_at;   -- wrong OTP tries for this code

-- "the newest code of this user + type" is looked up on every verify / resend
CREATE INDEX idx_verification_tokens_user_type ON verification_tokens (user_id, type, created_at);
