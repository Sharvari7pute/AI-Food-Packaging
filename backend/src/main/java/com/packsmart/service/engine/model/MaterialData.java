package com.packsmart.service.engine.model;

/**
 * Engine view of one material (materials.csv joined with material_extras.csv).
 * OTR in cc/(m²·day·atm) at 23 °C and WVTR in g/(m²·day) at 38 °C/90 % RH, both normalised to 25 µm.
 */
public record MaterialData(
        String name,
        String type,
        double otr25,
        double wvtr25,
        double costPerKgInr,
        double densityGCm3,
        double minTempC,
        double maxTempC,
        boolean recyclable,
        boolean biodegradable,
        boolean transparent,
        boolean heatSealable,
        int strength,
        String family,
        double co2eKgPerKg,
        boolean approx,
        String sourceUrl,
        String notes) {

    /** Rigid packs (glass, tinplate) are reference-only and never recommended as flexible packs. */
    public boolean isRigid() {
        return "rigid".equalsIgnoreCase(type);
    }
}
