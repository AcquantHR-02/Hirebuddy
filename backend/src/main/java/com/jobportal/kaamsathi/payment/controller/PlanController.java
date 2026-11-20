package com.jobportal.kaamsathi.payment.controller;

import com.jobportal.kaamsathi.payment.dto.PlanResponse;
import com.jobportal.kaamsathi.payment.service.PlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public "choose a plan" page (pricing page works before login). Open in SecurityConfig.
 */
@RestController
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /** GET /api/plans → 200 + active plans, cheapest first */
    @GetMapping("/api/plans")
    public List<PlanResponse> list() {
        return planService.listActive();
    }
}
