package com.packsmart.service.engine.model;

import java.util.List;

/** Combined properties of a layer stack (a single film is a one-layer stack). */
public record LaminateEval(
        double otr,
        double wvtr,
        double totalThicknessUm,
        double minTempC,
        double maxTempC,
        boolean transparent,
        boolean heatSealable,
        int strength,
        boolean biodegradable,
        boolean recyclable,
        String family,
        boolean containsAluminium,
        boolean approx) {

    public static final String MIXED = "mixed";

    public boolean monoMaterial() {
        return !MIXED.equals(family);
    }

    /** Names of layers, outside to inside. */
    public static List<String> names(List<Layer> layers) {
        return layers.stream().map(l -> l.material().name()).toList();
    }
}
