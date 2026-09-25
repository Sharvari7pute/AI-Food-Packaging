package com.packsmart.dto;

import java.time.Instant;
import java.util.List;

/** Engine result as returned to the UI (see MASTER_PROMPT Section 7). */
public record RecommendResponse(
        Long id,
        String shareId,
        String commodity,
        String commodityHi,
        boolean aiEstimatedFood,
        InputsEcho inputs,
        RequirementsDto requirements,
        Double requiredOtr,
        Double requiredWvtr,
        List<OptionDto> options,
        List<NearMissDto> nearMisses,
        AvoidDto avoid,
        MapDto map,
        String disclaimer,
        Instant createdAt) {

    /** Echo of the effective inputs (after overrides and area estimation). */
    public record InputsEcho(
            Long commodityId,
            String commodity,
            Double moisturePct,
            double waterActivity,
            Double criticalAw,
            double fatPct,
            String o2Sensitive,
            String lightSensitive,
            boolean respiring,
            Double respirationRate,
            Double ph,
            Double recommendedStorageTempMinC,
            Double recommendedStorageTempMaxC,
            String mainDeteriorationFactor,
            double packWeightG,
            double packAreaM2,
            boolean areaEstimated,
            int shelfLifeDays,
            String storageType,
            double storageTempC,
            double relativeHumidityPct,
            String transport,
            String priority,
            String language) {
    }

    public record RequirementsDto(
            String o2Barrier,
            String moistureMode,
            boolean opaque,
            boolean frozen,
            boolean needsMap,
            boolean needsStrength,
            List<String> reasons) {
    }

    public record LayerDto(String material, double thicknessUm) {
    }

    public record ScoresDto(double barrier, double cost, double eco, double strength, double total, Double topsis) {
    }

    public record CurvePointDto(int day, Double o2UsedPct, Double moistureUsedPct) {
    }

    public record OptionDto(
            int rank,
            String name,
            String kind,
            List<LayerDto> layers,
            double totalThicknessUm,
            double otr,
            double wvtr,
            ScoresDto scores,
            int estimatedShelfLifeDays,
            String limitingFactor,
            Double shelfLifeO2Days,
            Double shelfLifeMoistureDays,
            double costPer1000Inr,
            double co2eKgPer1000,
            double gramsPerPack,
            boolean recyclable,
            boolean biodegradable,
            String family,
            boolean transparent,
            boolean heatSealable,
            int strength,
            double minTempC,
            double maxTempC,
            boolean approxData,
            boolean perforationNeeded,
            List<String> reasons,
            List<CurvePointDto> curve) {
    }

    public record NearMissDto(
            String name,
            String kind,
            List<LayerDto> layers,
            double otr,
            double wvtr,
            int bestAchievableShelfLifeDays,
            String limitingFactor,
            double costPer1000Inr,
            List<String> reasons) {
    }

    public record AvoidDto(String name, String kind, double thicknessUm, double otr, double wvtr, String reason) {
    }

    public record GasMixDto(double o2Pct, double co2Pct, double n2Pct) {
    }

    public record MapDto(
            boolean mapTargetFound,
            Double targetO2Min,
            Double targetO2Max,
            Double targetCo2Min,
            Double targetCo2Max,
            Double storageTempC,
            GasMixDto gasMix,
            double respirationMlPerDay,
            double requiredOtr,
            String film,
            int filmThicknessUm,
            double filmOtr,
            boolean perforationNeeded,
            List<String> reasons,
            String note) {
    }
}
