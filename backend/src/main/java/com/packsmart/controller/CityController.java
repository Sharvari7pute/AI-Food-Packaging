package com.packsmart.controller;

import java.util.List;

import com.packsmart.dto.CatalogDtos.CityDto;
import com.packsmart.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CatalogService catalog;

    @GetMapping
    public List<CityDto> list() {
        return catalog.cities();
    }
}
