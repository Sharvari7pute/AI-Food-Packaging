package com.packsmart.controller;

import com.packsmart.service.ai.GeminiClient;
import com.packsmart.dto.CatalogDtos.HealthDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final GeminiClient ai;

    @GetMapping("/health")
    public HealthDto health() {
        return new HealthDto("ok", ai.isEnabled());
    }
}
