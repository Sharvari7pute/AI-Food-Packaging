package com.packsmart.controller;

import java.util.List;

import com.packsmart.dto.CatalogDtos.MaterialDto;
import com.packsmart.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final CatalogService catalog;

    @GetMapping
    public List<MaterialDto> list() {
        return catalog.materials();
    }
}
