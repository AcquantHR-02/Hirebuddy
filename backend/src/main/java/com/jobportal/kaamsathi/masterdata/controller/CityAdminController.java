package com.jobportal.kaamsathi.masterdata.controller;

import com.jobportal.kaamsathi.masterdata.dto.CityRequest;
import com.jobportal.kaamsathi.masterdata.dto.CityResponse;
import com.jobportal.kaamsathi.masterdata.service.CityService;
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
 * Admin manages the city list. ADMIN only (class-level @PreAuthorize).
 */
@RestController
@RequestMapping("/api/masterdata/cities")
@PreAuthorize("hasRole('ADMIN')")
public class CityAdminController {

    private final CityService cityService;

    public CityAdminController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping("/all")
    public List<CityResponse> all() {
        return cityService.listAll();
    }

    /** POST /api/masterdata/cities {"name":"Pune","state":"Maharashtra"} → 201 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse create(@Valid @RequestBody CityRequest request) {
        return cityService.create(request.name(), request.state());
    }

    /** PUT /api/masterdata/cities/{id} {"name":"...","state":"..."} → 200 */
    @PutMapping("/{id}")
    public CityResponse update(@PathVariable UUID id, @Valid @RequestBody CityRequest request) {
        return cityService.update(id, request.name(), request.state());
    }

    @PostMapping("/{id}/deactivate")
    public CityResponse deactivate(@PathVariable UUID id) {
        return cityService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public CityResponse activate(@PathVariable UUID id) {
        return cityService.activate(id);
    }
}
