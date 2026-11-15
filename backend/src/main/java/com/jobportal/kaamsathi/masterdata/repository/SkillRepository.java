package com.jobportal.kaamsathi.masterdata.repository;

import com.jobportal.kaamsathi.masterdata.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * DB access for the `skills` table. Only the masterdata module uses this.
 */
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    // dropdown / search box: active skills whose name contains q, A→Z, at most 50
    // SELECT * FROM skills WHERE active = TRUE AND name LIKE %q% ORDER BY name LIMIT 50
    List<Skill> findTop50ByActiveTrueAndNameContainingOrderByNameAsc(String q);

    // admin screen: every skill (active + hidden), A→Z
    List<Skill> findAllByOrderByNameAsc();

    // add: is the name taken? (case-insensitive thanks to the DB collation)
    boolean existsByName(String name);

    // rename: is the name taken by ANOTHER skill?
    boolean existsByNameAndIdNot(String name, UUID id);
}
