package com.jobportal.kaamsathi.masterdata.repository;

import com.jobportal.kaamsathi.masterdata.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * DB access for the `cities` table. Like SkillRepository, but duplicates are checked on (name, state).
 */
public interface CityRepository extends JpaRepository<City, UUID> {

    // dropdown: active cities whose NAME contains q, sorted by name then state, max 50
    List<City> findTop50ByActiveTrueAndNameContainingOrderByNameAscStateAsc(String q);

    // admin screen: every city (hidden too)
    List<City> findAllByOrderByNameAscStateAsc();

    // add: is this (name, state) pair taken?
    boolean existsByNameAndState(String name, String state);

    // update: is this pair taken by ANOTHER city?
    boolean existsByNameAndStateAndIdNot(String name, String state, UUID id);

    // job posting: does this city exist AND is it active?
    boolean existsByIdAndActiveTrue(UUID id);

    // how many of these ids are ACTIVE cities? (all ok ⇔ count == ids.size())
    long countByIdInAndActiveTrue(Collection<UUID> ids);
}
