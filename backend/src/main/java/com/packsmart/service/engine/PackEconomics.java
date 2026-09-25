package com.packsmart.service.engine;

import java.util.List;

import com.packsmart.service.engine.model.Economics;
import com.packsmart.service.engine.model.Layer;
import org.springframework.stereotype.Service;

/** Step E - material grams, cost and carbon per pack. Pure. */
@Service
public class PackEconomics {

    private static final double PACKS_PER_BATCH = 1000.0;
    private static final double GRAMS_PER_KG = 1000.0;

    /** {@code grams_per_pack = area_m2 × thickness_um × density_g_cm3} (the units give grams directly). */
    public double grams(double areaM2, double thicknessUm, double densityGCm3) {
        return areaM2 * thicknessUm * densityGCm3;
    }

    /**
     * {@code cost_per_pack = Σ grams × cost_per_kg / 1000}; {@code cost_per_1000 = cost_per_pack × 1000};
     * {@code co2e_kg_per_1000 = Σ grams × co2e_kg_per_kg}.
     */
    public Economics compute(List<Layer> layers, double areaM2) {
        double grams = 0;
        double cost = 0;
        double co2 = 0;
        for (Layer l : layers) {
            double g = grams(areaM2, l.thicknessUm(), l.material().densityGCm3());
            grams += g;
            cost += g * l.material().costPerKgInr() / GRAMS_PER_KG;
            co2 += g * l.material().co2eKgPerKg();
        }
        return new Economics(grams, cost, cost * PACKS_PER_BATCH, co2);
    }
}
