package com.jobportal.kaamsathi.payment.service;

import com.jobportal.kaamsathi.common.exception.ApiException;
import com.jobportal.kaamsathi.common.exception.ErrorCode;
import com.jobportal.kaamsathi.payment.dto.PlanRequest;
import com.jobportal.kaamsathi.payment.dto.PlanResponse;
import com.jobportal.kaamsathi.payment.entity.Plan;
import com.jobportal.kaamsathi.payment.repository.PlanRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Plan rules. Everyone can see active plans; only the admin adds / edits / hides (checked in the controller).
 */
@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final Clock clock;

    public PlanService(PlanRepository planRepository, Clock clock) {
        this.planRepository = planRepository;
        this.clock = clock;
    }

    // public "choose a plan" page: only plans you can buy, cheapest first
    @Transactional(readOnly = true)
    public List<PlanResponse> listActive() {
        return planRepository.findByActiveTrueOrderByPriceRupeesAsc().stream()
                .map(PlanResponse::from)
                .toList();
    }

    // admin screen: all plans, hidden ones too
    @Transactional(readOnly = true)
    public List<PlanResponse> listAll() {
        return planRepository.findAllByOrderByPriceRupeesAsc().stream()
                .map(PlanResponse::from)
                .toList();
    }

    @Transactional
    public PlanResponse create(PlanRequest request) {
        String name = request.name().trim();                // " Pro " → "Pro"
        if (planRepository.existsByName(name)) {            // friendly check (case-insensitive in MySQL)
            throw new ApiException(ErrorCode.PLAN_NAME_EXISTS);
        }
        Plan plan = new Plan(UUID.randomUUID(), name,
                request.priceRupees(), request.durationDays(),
                request.jobSlots(), request.recruiterSeats(), clock.instant());
        try {
            planRepository.saveAndFlush(plan);              // INSERT now → a race fails inside try
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.PLAN_NAME_EXISTS);
        }
        return PlanResponse.from(plan);
    }

    // full replace; companies that already bought this plan keep their own copied limits (4.3)
    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {
        Plan plan = find(id);
        String name = request.name().trim();
        if (planRepository.existsByNameAndIdNot(name, id)) {   // same name on ANOTHER plan?
            throw new ApiException(ErrorCode.PLAN_NAME_EXISTS);
        }
        plan.update(name, request.priceRupees(), request.durationDays(),
                request.jobSlots(), request.recruiterSeats(), clock.instant());
        try {
            planRepository.flush();                         // UPDATE now → a race fails inside try
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.PLAN_NAME_EXISTS);
        }
        return PlanResponse.from(plan);
    }

    @Transactional
    public PlanResponse activate(UUID id) {
        Plan plan = find(id);
        plan.activate(clock.instant());                     // dirty checking writes the UPDATE
        return PlanResponse.from(plan);
    }

    @Transactional
    public PlanResponse deactivate(UUID id) {
        Plan plan = find(id);
        plan.deactivate(clock.instant());
        return PlanResponse.from(plan);
    }

    // ---- for PaymentService (same module, so the entity may be returned) ----

    // the owner wants to buy this plan: must exist AND be active (hidden plan = can't buy → 404)
    @Transactional(readOnly = true)
    Plan requireBuyable(UUID id) {
        return planRepository.findById(id)
                .filter(Plan::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.PLAN_NOT_FOUND));
    }

    // id → name for lists (billing history, admin queue); hidden plans too (old requests keep their name)
    @Transactional(readOnly = true)
    Map<UUID, String> namesByIds(Collection<UUID> ids) {
        return planRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Plan::getId, Plan::getName));
    }

    private Plan find(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PLAN_NOT_FOUND));
    }
}
