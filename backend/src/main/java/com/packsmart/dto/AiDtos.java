package com.packsmart.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Requests and responses of the AI layer. {@code aiUsed = false} means a non-AI fallback answered. */
public final class AiDtos {

    private AiDtos() {
    }

    public record ParseRequest(@NotBlank @Size(max = 1000) String query) {
    }

    public record ParseResponse(RecommendRequest draft, String commodityName, List<String> missingFields,
                                boolean aiUsed, String note) {
    }

    public record ExplainRequest(Long recommendationId, String shareId,
                                 @Pattern(regexp = "en|hi|mr", message = "must be en, hi or mr") String language) {
    }

    public record ExplainResponse(String text, String language, boolean aiUsed) {
    }

    public record ChatMessage(@Pattern(regexp = "user|assistant") String role, @NotBlank @Size(max = 4000) String content) {
    }

    public record ChatRequest(@NotEmpty @Size(max = 30) List<@Valid ChatMessage> messages,
                              @Pattern(regexp = "en|hi|mr", message = "must be en, hi or mr") String language,
                              Long recommendationId) {
    }

    public record ChatResponse(String reply, boolean aiUsed) {
    }

    public record EstimateFoodRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record EstimateFoodResponse(CustomCommodity food, Long existingCommodityId, boolean aiEstimated,
                                       boolean aiUsed, String note) {
    }
}
