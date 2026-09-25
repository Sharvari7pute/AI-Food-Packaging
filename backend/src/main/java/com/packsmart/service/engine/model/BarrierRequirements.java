package com.packsmart.service.engine.model;

import java.util.List;

/**
 * Step B output. {@code requiredOtr}/{@code requiredWvtr} are maximum allowed values (null = no requirement).
 * The intermediate quantities are kept so shelf life can be computed with the same numbers.
 */
public record BarrierRequirements(
        Double requiredOtr,
        Double requiredWvtr,
        double o2ToleranceMl,
        double allowedWaterG,
        double drivingFactor,
        double otrTempFactor,
        double wvtrTempFactor,
        List<String> reasons) {

    /** No barrier requirement at all (e.g. respiring produce, handled by MAP). */
    public static BarrierRequirements none(List<String> reasons) {
        return new BarrierRequirements(null, null, 0, 0, 0, 1, 1, reasons);
    }

    public boolean hasOtr() {
        return requiredOtr != null;
    }

    public boolean hasWvtr() {
        return requiredWvtr != null;
    }
}
