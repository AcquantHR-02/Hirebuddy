package com.jobportal.kaamsathi.masterdata.repository;

import com.jobportal.kaamsathi.masterdata.entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * DB access for the `industries` table. Same queries as SkillRepository.
 */
public interface IndustryRepository extends JpaRepository<Industry, UUID> {

    List<Industry> findTop50ByActiveTrueAndNameContainingOrderByNameAsc(String q);

    List<Industry> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
