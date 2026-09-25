package com.packsmart.service.engine.model;

/** Effective food properties for one request (commodity values with any overrides applied). */
public record FoodProfile(
        Long commodityId,
        String name,
        String nameHi,
        String category,
        Double moisturePct,
        double waterActivity,
        Double criticalAw,
        double fatPct,
        String o2Sensitive,
        String lightSensitive,
        boolean respiring,
        Double respirationRate,
        Integer defaultShelfLifeDays,
        boolean aiEstimated) {
}
