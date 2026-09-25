package com.packsmart.controller;

import java.util.List;

import com.packsmart.dto.CatalogDtos.CommodityDto;
import com.packsmart.dto.CatalogDtos.CommoditySummary;
import com.packsmart.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commodities")
@RequiredArgsConstructor
public class CommodityController {

    private final CatalogService catalog;

    @GetMapping
    public List<CommoditySummary> list() {
        return catalog.commoditySummaries();
    }

    @GetMapping("/{id}")
    public CommodityDto get(@PathVariable Long id) {
        return catalog.commodity(id);
    }
}
