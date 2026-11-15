package com.jobportal.kaamsathi.masterdata.controller;

import com.jobportal.kaamsathi.masterdata.dto.SkillResponse;
import com.jobportal.kaamsathi.masterdata.service.SkillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public skill list for dropdowns / search boxes. No login needed (SecurityConfig permits GET /api/masterdata/*).
 */
@RestController
@RequestMapping("/api/masterdata/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /** GET /api/masterdata/skills?q=ja → 200 + active skills containing "ja" (q optional) */
    @GetMapping
    public List<SkillResponse> search(@RequestParam(required = false) String q) {
        return skillService.search(q);
    }
}
