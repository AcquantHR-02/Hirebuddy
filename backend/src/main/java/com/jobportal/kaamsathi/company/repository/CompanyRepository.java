package com.jobportal.kaamsathi.company.repository;

import com.jobportal.kaamsathi.company.entity.Company;
import com.jobportal.kaamsathi.company.entity.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DB access for the `companies` table. Only the company module uses this;
 * other modules go through company's service/ layer.
 */
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    // SELECT * FROM companies WHERE owner_id = ?   ("my company"; also: does this owner already have one?)
    Optional<Company> findByOwnerId(UUID ownerId);

    // SELECT 1 FROM companies WHERE gstin = ? LIMIT 1   (register: is this GSTIN already used?)
    boolean existsByGstin(String gstin);

    // SELECT 1 ... WHERE gstin = ? AND id <> ?   (update: GSTIN used by ANOTHER company?)
    boolean existsByGstinAndIdNot(String gstin, UUID id);

    // SELECT * FROM companies WHERE status = ? ORDER BY created_at   (admin review queue: oldest first)
    List<Company> findByStatusOrderByCreatedAtAsc(CompanyStatus status);
}
