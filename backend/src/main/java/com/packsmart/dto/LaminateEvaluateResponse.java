package com.packsmart.dto;

import java.util.List;

/** Laminate Builder output. {@code test} is null unless a food test was requested. */
public record LaminateEvaluateResponse(
        double otr,
        double wvtr,
        double totalThicknessUm,
        double areaM2,
        double gramsPerPack,
        double costPer1000Inr,
        double co2eKgPer1000,
        boolean recyclable,
        boolean biodegradable,
        String family,
        boolean transparent,
        boolean heatSealable,
        int strength,
        double minTempC,
        double maxTempC,
        boolean approx,
        FoodTestResult test) {

    public record FoodTestResult(
            String commodity,
            boolean passes,
            Double requiredOtr,
            Double requiredWvtr,
            Double mapRequiredOtr,
            int estimatedShelfLifeDays,
            String limitingFactor,
            List<String> failReasons,
            List<String> requirementReasons) {
    }
}
