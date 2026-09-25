package com.packsmart.controller;

import com.packsmart.dto.HistoryDtos.PageDto;
import com.packsmart.dto.HistoryDtos.RecommendationSummary;
import com.packsmart.dto.RecommendRequest;
import com.packsmart.dto.RecommendResponse;
import com.packsmart.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendationService recommendations;

    /** Runs the engine and saves the result. */
    @PostMapping("/recommend")
    @ResponseStatus(HttpStatus.OK)
    public RecommendResponse recommend(@Valid @RequestBody RecommendRequest req) {
        return recommendations.recommend(req);
    }

    /** History, latest first. */
    @GetMapping("/recommendations")
    public PageDto<RecommendationSummary> history(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return recommendations.history(page, size);
    }

    @GetMapping("/recommendations/{id}")
    public RecommendResponse get(@PathVariable Long id) {
        return recommendations.get(id);
    }

    /** Public, read-only result for the QR verify page. */
    @GetMapping("/recommendations/share/{shareId}")
    public RecommendResponse shared(@PathVariable String shareId) {
        return recommendations.getByShareId(shareId);
    }
}
