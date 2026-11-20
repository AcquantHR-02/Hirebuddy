package com.jobportal.kaamsathi.auth.repository;

import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.auth.entity.UserStatus;
import com.jobportal.kaamsathi.common.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DB access for the `users` table. Only the auth module uses this;
 * other modules go through auth's service/ layer.
 * Spring Data writes the code for us at startup (from the method names).
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    // SELECT * FROM users WHERE email = ?   (login)
    Optional<User> findByEmail(String email);

    // SELECT 1 FROM users WHERE email = ? LIMIT 1   (signup: is the email taken?)
    boolean existsByEmail(String email);

    // SELECT * FROM users WHERE company_id = ? AND role = ? ORDER BY created_at   (team list)
    List<User> findByCompanyIdAndRoleOrderByCreatedAtAsc(UUID companyId, Role role);

    // SELECT * FROM users WHERE role = ? ORDER BY created_at   (admin list: oldest = first admin on top)
    List<User> findByRoleOrderByCreatedAtAsc(Role role);

    // admin "Users" screen: every filter optional (null = off); q = part of the email. Newest first.
    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:q IS NULL OR u.email LIKE CONCAT('%', :q, '%'))
            ORDER BY u.createdAt DESC
            """)
    Page<User> search(@Param("role") Role role, @Param("status") UserStatus status,
                      @Param("q") String q, Pageable pageable);
}
