package com.packsmart.dto;

import java.util.List;

/** Read-only catalog views. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CommoditySummary(Long id, String name, String nameHi, String category, boolean respiring) {
    }

    public record CommodityDto(
            Long id,
            String name,
            String nameHi,
            String category,
            Double moisturePct,
            Double waterActivity,
            Double criticalAw,
            Double fatPct,
            String o2Sensitive,
            String lightSensitive,
            boolean respiring,
            Double respirationRate,
            Integer defaultShelfLifeDays,
            Double ph,
            Double storageTempMinC,
            Double storageTempMaxC,
            String mainDeteriorationFactor,
            String sourceUrl,
            String notes,
            boolean approx) {
    }

    public record MaterialDto(
            Long id,
            String name,
            String type,
            Double otr25um,
            Double wvtr25um,
            Double costPerKgInr,
            Double densityGCm3,
            Double minTempC,
            Double maxTempC,
            Boolean recyclable,
            Boolean biodegradable,
            Boolean transparent,
            Boolean heatSealable,
            Integer strength,
            String sourceUrl,
            String notes,
            String family,
            Double co2eKgPerKg,
            boolean approx,
            boolean rigid,
            Double ecoScore,
            Double referenceCostPer1000Inr,
            Double referenceCo2eKgPer1000) {
    }

    public record LayerView(String material, double thicknessUm) {
    }

    public record LaminateDto(
            Long id,
            String name,
            String typicalUse,
            List<LayerView> layers,
            double totalThicknessUm,
            double otr,
            double wvtr,
            double minTempC,
            double maxTempC,
            boolean transparent,
            boolean heatSealable,
            int strength,
            boolean recyclable,
            boolean biodegradable,
            String family,
            boolean approx,
            double referenceAreaM2,
            double costPer1000Inr,
            double co2eKgPer1000,
            double ecoScore) {
    }

    public record CityDto(
            Long id,
            String name,
            String state,
            Double summerTempC,
            Double summerRhPct,
            Double monsoonTempC,
            Double monsoonRhPct,
            Double winterTempC,
            Double winterRhPct) {
    }

    public record HealthDto(String status, boolean aiEnabled) {
    }
}
