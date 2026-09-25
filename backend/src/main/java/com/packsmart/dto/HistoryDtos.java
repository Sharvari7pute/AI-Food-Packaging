package com.packsmart.dto;

import java.time.Instant;
import java.util.List;

public final class HistoryDtos {

    private HistoryDtos() {
    }

    public record RecommendationSummary(
            Long id,
            String shareId,
            String commodity,
            String topOption,
            Integer estimatedShelfLifeDays,
            Instant createdAt) {
    }

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
