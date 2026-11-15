package com.jobportal.kaamsathi.masterdata.controller;

import com.jobportal.kaamsathi.masterdata.dto.JobCategoryResponse;
import com.jobportal.kaamsathi.masterdata.service.JobCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public job category list for dropdowns. No login needed (GET /api/masterdata/*).
 */
@RestController
@RequestMapping("/api/masterdata/categories")
public class JobCategoryController {

    private final JobCategoryService categoryService;

    public JobCategoryController(JobCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** GET /api/masterdata/categories?q=soft → 200 + active categories (q optional) */
    @GetMapping
    public List<JobCategoryResponse> search(@RequestParam(required = false) String q) {
        return categoryService.search(q);
    }
}
