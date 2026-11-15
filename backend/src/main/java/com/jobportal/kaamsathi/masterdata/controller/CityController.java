package com.jobportal.kaamsathi.masterdata.controller;

import com.jobportal.kaamsathi.masterdata.dto.CityResponse;
import com.jobportal.kaamsathi.masterdata.service.CityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public city list for dropdowns. No login needed (GET /api/masterdata/*).
 */
@RestController
@RequestMapping("/api/masterdata/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    /** GET /api/masterdata/cities?q=pun → 200 + active cities whose name contains "pun" (q optional) */
    @GetMapping
    public List<CityResponse> search(@RequestParam(required = false) String q) {
        return cityService.search(q);
    }
}
