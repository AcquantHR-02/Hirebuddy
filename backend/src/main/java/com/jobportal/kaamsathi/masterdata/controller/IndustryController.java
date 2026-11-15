package com.jobportal.kaamsathi.masterdata.controller;

import com.jobportal.kaamsathi.masterdata.dto.IndustryResponse;
import com.jobportal.kaamsathi.masterdata.service.IndustryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public industry list for dropdowns. No login needed (GET /api/masterdata/*).
 */
@RestController
@RequestMapping("/api/masterdata/industries")
public class IndustryController {

    private final IndustryService industryService;

    public IndustryController(IndustryService industryService) {
        this.industryService = industryService;
    }

    /** GET /api/masterdata/industries?q=bank → 200 + active industries (q optional) */
    @GetMapping
    public List<IndustryResponse> search(@RequestParam(required = false) String q) {
        return industryService.search(q);
    }
}
