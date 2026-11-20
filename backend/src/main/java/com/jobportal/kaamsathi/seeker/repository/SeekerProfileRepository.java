package com.jobportal.kaamsathi.seeker.repository;

import com.jobportal.kaamsathi.seeker.entity.SeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * DB access for seeker_profiles (+ child tables). Only the seeker module uses this.
 */
public interface SeekerProfileRepository extends JpaRepository<SeekerProfile, UUID> {
}
