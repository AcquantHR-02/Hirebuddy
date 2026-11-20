package com.jobportal.kaamsathi.masterdata.repository;

import com.jobportal.kaamsathi.masterdata.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * DB access for the `job_categories` table. Same queries as SkillRepository.
 */
public interface JobCategoryRepository extends JpaRepository<JobCategory, UUID> {

    List<JobCategory> findTop50ByActiveTrueAndNameContainingOrderByNameAsc(String q);

    List<JobCategory> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    // job posting: does this category exist AND is it active?
    boolean existsByIdAndActiveTrue(UUID id);
}
