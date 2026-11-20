package com.jobportal.kaamsathi.payment.repository;

import com.jobportal.kaamsathi.payment.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * DB access for the `plans` table. Only the payment module uses this.
 */
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    // public "choose a plan" page: only plans you can buy, cheapest first
    // SELECT * FROM plans WHERE active = TRUE ORDER BY price_rupees
    List<Plan> findByActiveTrueOrderByPriceRupeesAsc();

    // admin screen: every plan (active + hidden), cheapest first
    List<Plan> findAllByOrderByPriceRupeesAsc();

    // add: is the name taken? (case-insensitive thanks to the DB collation)
    boolean existsByName(String name);

    // edit: is the name taken by ANOTHER plan?
    boolean existsByNameAndIdNot(String name, UUID id);
}
