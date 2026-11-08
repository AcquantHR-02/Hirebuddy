package com.jobportal.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Database access for User rows. We only write the method NAMES;
 * Spring Data builds the SQL for us when the app starts.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    // SELECT COUNT(*) > 0 FROM users WHERE email = ?   (signup: "is this email taken?")
    boolean existsByEmail(String email);

    // SELECT * FROM users WHERE email = ?              (login: "find this account")
    Optional<User> findByEmail(String email);
}
