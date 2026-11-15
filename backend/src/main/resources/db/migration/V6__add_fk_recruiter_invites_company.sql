-- V6: companies exist now (V5), so an invite must point to a real company.
-- V4 stays untouched (never edit a migration that has run); we add the FK here.
-- ON DELETE CASCADE: if a company is ever removed, its open invites go with it.

ALTER TABLE recruiter_invites
    ADD CONSTRAINT fk_recruiter_invites_company FOREIGN KEY (company_id)
        REFERENCES companies (id) ON DELETE CASCADE;
