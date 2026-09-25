package com.packsmart.controller;

import java.util.List;

import com.packsmart.dto.CatalogDtos.LaminateDto;
import com.packsmart.dto.LaminateEvaluateRequest;
import com.packsmart.dto.LaminateEvaluateResponse;
import com.packsmart.service.CatalogService;
import com.packsmart.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/laminates")
@RequiredArgsConstructor
public class LaminateController {

    private final CatalogService catalog;
    private final RecommendationService recommendations;

    @GetMapping
    public List<LaminateDto> list() {
        return catalog.laminates();
    }

    /** Laminate Builder: custom layers → OTR, WVTR, cost, CO2e, recyclability (+ optional food test). */
    @PostMapping("/evaluate")
    public LaminateEvaluateResponse evaluate(@Valid @RequestBody LaminateEvaluateRequest req) {
        return recommendations.evaluateLayers(req);
    }
}
