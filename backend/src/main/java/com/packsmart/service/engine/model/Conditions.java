package com.packsmart.service.engine.model;

/** Pack and storage conditions for one request. {@code areaM2} is already resolved (given or estimated). */
public record Conditions(
        double packWeightG,
        double areaM2,
        boolean areaEstimated,
        int shelfLifeDays,
        StorageType storageType,
        double storageTempC,
        double relativeHumidityPct,
        Transport transport,
        Priority priority) {

    public double packWeightKg() {
        return packWeightG / 1000.0;
    }
}
