package com.packsmart.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All tunable engine constants ({@code engine.*} in application.yml).
 * Defaults mirror application.yml so pure engine services can be unit-tested with {@code new EngineProperties()}.
 */
@Data
@ConfigurationProperties(prefix = "engine")
public class EngineProperties {

    private double referenceOtrTempC = 23;
    private double referenceWvtrTempC = 38;
    private double referenceRespirationTempC = 20;
    private double q10 = 2.0;
    private double o2PartialPressureAtm = 0.21;
    private double referenceThicknessUm = 25;
    private Map<String, Double> o2ToleranceMlPerKg = new HashMap<>(Map.of("high", 1.0, "medium", 10.0, "low", 50.0));
    private double allowedMoistureGainFraction = 0.02;
    private double keepOutAwThreshold = 0.7;
    private double keepInAwThreshold = 0.9;
    private List<Integer> thicknessOptionsUm = new ArrayList<>(List.of(12, 15, 20, 25, 30, 40, 50, 60, 75, 100));
    private int minThicknessUm = 20;
    private int maxThicknessUm = 100;
    private int maxShelfLifeDays = 730;
    private double overkillMargin = 20;
    private Map<String, Weights> scoreWeights = new HashMap<>(Map.of(
            "default", new Weights(0.40, 0.25, 0.20, 0.15),
            "eco", new Weights(0.30, 0.15, 0.40, 0.15),
            "budget", new Weights(0.30, 0.45, 0.10, 0.15)));

    private double fatThresholdPct = 10;
    private double drivingFactorDivisor = 90;
    private double minDrivingFactor = 0.05;
    private double frozenMinTempC = -25;
    private int minStrengthLongDistance = 3;
    private int strengthScale = 5;
    private double areaBaseM2 = 0.02;
    private double areaPerGramM2 = 0.0004;
    private String avoidMaterial = "LDPE";
    private int avoidThicknessUm = 50;
    private String perforatedFallbackMaterial = "LDPE";
    private int perforatedFallbackThicknessUm = 25;
    private double curveExtension = 1.5;
    private int curveMaxPoints = 60;
    private double defaultMapTargetO2Pct = 5;
    private int topOptions = 3;
    /** "topsis" (default) or "weighted" - how passing non-MAP packs are ranked. */
    private String rankingMethod = "topsis";
    private int nearMissCount = 3;
    private BarrierScore barrierScore = new BarrierScore();
    private EcoScore ecoScore = new EcoScore();

    /** Score weights for one priority. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Weights {
        private double barrier;
        private double cost;
        private double eco;
        private double strength;
    }

    @Data
    public static class BarrierScore {
        private double withinMargin = 1.0;
        private double overkill = 0.8;
    }

    @Data
    public static class EcoScore {
        private double biodegradable = 1.0;
        private double recyclableMono = 0.8;
        private double containsAluminium = 0.1;
        private double other = 0.3;
        private String aluminiumFamily = "AL";
    }

    /** Weights for a priority name (DEFAULT/ECO/BUDGET), falling back to "default". */
    public Weights weightsFor(String priority) {
        Weights w = scoreWeights.get(priority.toLowerCase());
        return w != null ? w : scoreWeights.get("default");
    }
}
