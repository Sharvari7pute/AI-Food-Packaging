package com.packsmart.controller;

import com.packsmart.dto.AiDtos.ChatRequest;
import com.packsmart.dto.AiDtos.ChatResponse;
import com.packsmart.dto.AiDtos.EstimateFoodRequest;
import com.packsmart.dto.AiDtos.EstimateFoodResponse;
import com.packsmart.dto.AiDtos.ExplainRequest;
import com.packsmart.dto.AiDtos.ExplainResponse;
import com.packsmart.dto.AiDtos.ParseRequest;
import com.packsmart.dto.AiDtos.ParseResponse;
import com.packsmart.service.ai.ChatService;
import com.packsmart.service.ai.ExplainService;
import com.packsmart.service.ai.FoodEstimatorService;
import com.packsmart.service.ai.QueryParserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI helpers. Gemini never picks materials or numbers; every endpoint works without a key. */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final QueryParserService parser;
    private final ExplainService explainer;
    private final ChatService chat;
    private final FoodEstimatorService estimator;

    @PostMapping("/parse")
    public ParseResponse parse(@Valid @RequestBody ParseRequest req) {
        return parser.parse(req.query());
    }

    @PostMapping("/explain")
    public ExplainResponse explain(@Valid @RequestBody ExplainRequest req) {
        if (req.recommendationId() == null && req.shareId() == null) {
            throw new IllegalArgumentException("recommendationId or shareId is required");
        }
        return explainer.explain(req.recommendationId(), req.shareId(), req.language());
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        return chat.chat(req.messages(), req.language(), req.recommendationId());
    }

    @PostMapping("/estimate-food")
    public EstimateFoodResponse estimateFood(@Valid @RequestBody EstimateFoodRequest req) {
        return estimator.estimate(req.name());
    }
}
