-- V13: profile picture for EVERY user (seeker, owner, recruiter, admin).
-- The picture itself is a row in `files` (kind PHOTO); users only points to it.
-- ON DELETE SET NULL: if the file row is removed, the user simply has no photo (no broken link).
-- NEVER edit this file after it has run. Add V14__... for changes.

ALTER TABLE users
    ADD COLUMN photo_file_id CHAR(36) NULL AFTER company_id,
    ADD CONSTRAINT fk_users_photo_file FOREIGN KEY (photo_file_id) REFERENCES files (id) ON DELETE SET NULL;
