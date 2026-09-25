package com.packsmart.controller;

import com.packsmart.dto.RecommendRequest;
import com.packsmart.dto.RecommendResponse;
import com.packsmart.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SimulateController {

    private final RecommendationService recommendations;

    /** What-if simulator: same engine, nothing saved. */
    @PostMapping("/simulate")
    public RecommendResponse simulate(@Valid @RequestBody RecommendRequest req) {
        return recommendations.simulate(req);
    }
}
