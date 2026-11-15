package com.jobportal.kaamsathi.masterdata.controller;

import com.jobportal.kaamsathi.masterdata.dto.JobCategoryResponse;
import com.jobportal.kaamsathi.masterdata.dto.MasterItemRequest;
import com.jobportal.kaamsathi.masterdata.service.JobCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin manages the job category list. ADMIN only (class-level @PreAuthorize).
 */
@RestController
@RequestMapping("/api/masterdata/categories")
@PreAuthorize("hasRole('ADMIN')")
public class JobCategoryAdminController {

    private final JobCategoryService categoryService;

    public JobCategoryAdminController(JobCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/all")
    public List<JobCategoryResponse> all() {
        return categoryService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobCategoryResponse create(@Valid @RequestBody MasterItemRequest request) {
        return categoryService.create(request.name());
    }

    @PutMapping("/{id}")
    public JobCategoryResponse rename(@PathVariable UUID id, @Valid @RequestBody MasterItemRequest request) {
        return categoryService.rename(id, request.name());
    }

    @PostMapping("/{id}/deactivate")
    public JobCategoryResponse deactivate(@PathVariable UUID id) {
        return categoryService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public JobCategoryResponse activate(@PathVariable UUID id) {
        return categoryService.activate(id);
    }
}
