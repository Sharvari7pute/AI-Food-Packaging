package com.packsmart.service.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.packsmart.config.EngineProperties;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.MapResult;
import com.packsmart.service.engine.model.MapTargetData;
import com.packsmart.service.engine.model.MaterialData;
import com.packsmart.service.engine.model.ThicknessChoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Step H - modified atmosphere packaging (MAP) for respiring produce. Pure. */
@Service
@RequiredArgsConstructor
public class MapCalculator {

    private static final double HOURS_PER_DAY = 24.0;
    private static final double PERCENT = 100.0;

    private final EngineProperties props;
    private final BarrierCalculator barrier;
    private final ThicknessCalculator thickness;

    /** {@code RR_total_mL_per_day = respiration_rate × packWeightKg × 24 × tf(T, 20)}. */
    public double respirationMlPerDay(double respirationRate, double packWeightKg, double tempC) {
        return respirationRate * packWeightKg * HOURS_PER_DAY
                * barrier.temperatureFactor(tempC, props.getReferenceRespirationTempC());
    }

    /** {@code OTR_required_map = RR_total_mL_per_day / (area × (0.21 − targetO2 / 100))}. */
    public double requiredOtr(double respirationMlPerDay, double areaM2, double targetO2Pct) {
        double gradient = props.getO2PartialPressureAtm() - targetO2Pct / PERCENT;
        if (gradient <= 0) {
            throw new IllegalArgumentException("Target O₂ must be below air (" + Num.fmt(props.getO2PartialPressureAtm() * PERCENT) + "%)");
        }
        return respirationMlPerDay / (areaM2 * gradient);
    }

    /**
     * Thickest allowed gauge whose OTR is still ≥ the required OTR, i.e. the OTR closest to the requirement from above
     * (too much OTR lets in excess oxygen). Fails if even the thinnest gauge is not breathable enough.
     */
    public ThicknessChoice chooseThickness(MaterialData m, double requiredOtr) {
        List<Integer> options = thickness.allowedThicknesses();
        for (int i = options.size() - 1; i >= 0; i--) {
            int t = options.get(i);
            if (thickness.otrAt(m, t) >= requiredOtr) {
                return new ThicknessChoice(t, true, t + " µm gives OTR " + Num.fmt(thickness.otrAt(m, t))
                        + " ≥ required " + Num.fmt(requiredOtr) + " (breathable enough)");
            }
        }
        int thinnest = options.get(0);
        return new ThicknessChoice(thinnest, false, "even at " + thinnest + " µm OTR is "
                + Num.fmt(thickness.otrAt(m, thinnest)) + " < required " + Num.fmt(requiredOtr) + " → produce would suffocate");
    }

    /** Selects the breathable film for the produce, or LDPE + micro-perforations when no film breathes enough. */
    public MapResult compute(FoodProfile food, Conditions cond, List<MaterialData> materials, Optional<MapTargetData> target) {
        List<String> reasons = new ArrayList<>();
        String note = null;
        double targetO2;
        if (target.isPresent()) {
            MapTargetData t = target.get();
            targetO2 = (t.o2Min() + t.o2Max()) / 2.0;
            reasons.add("Target atmosphere for " + food.name() + ": O₂ " + Num.fmt(t.o2Min()) + "–" + Num.fmt(t.o2Max())
                    + "%, CO₂ " + Num.fmt(t.co2Min()) + "–" + Num.fmt(t.co2Max()) + "%");
        } else {
            targetO2 = props.getDefaultMapTargetO2Pct();
            note = "No MAP target data for " + food.name() + " - using a generic " + Num.fmt(targetO2) + "% O₂ target. Verify with literature.";
            reasons.add(note);
        }

        double rate = food.respirationRate() != null ? food.respirationRate() : 0;
        if (food.respirationRate() == null) {
            String missing = "Respiration rate unknown for " + food.name() + " - enter it to size the film properly.";
            note = note == null ? missing : note + " " + missing;
        }
        double rr = respirationMlPerDay(rate, cond.packWeightKg(), cond.storageTempC());
        double reqOtr = requiredOtr(rr, cond.areaM2(), targetO2);
        reasons.add("Produce uses " + Num.fmt(rr) + " mL O₂/day at " + Num.fmt(cond.storageTempC()) + " °C → film OTR must be ≥ "
                + Num.fmt(reqOtr) + " cc/m²·day·atm to hold ~" + Num.fmt(targetO2) + "% O₂");

        String bestName = null;
        int bestT = 0;
        double bestOtr = Double.POSITIVE_INFINITY;
        for (MaterialData m : materials) {
            if (m.isRigid() || !m.heatSealable()) {
                continue;
            }
            ThicknessChoice c = chooseThickness(m, reqOtr);
            double otr = thickness.otrAt(m, c.thicknessUm());
            if (c.passes() && otr < bestOtr) {
                bestName = m.name();
                bestT = c.thicknessUm();
                bestOtr = otr;
            }
        }
        boolean perforated = bestName == null;
        if (perforated) {
            bestName = props.getPerforatedFallbackMaterial();
            bestT = props.getPerforatedFallbackThicknessUm();
            Optional<MaterialData> fallback = materials.stream().filter(m -> m.name().equalsIgnoreCase(props.getPerforatedFallbackMaterial())).findFirst();
            bestOtr = fallback.map(m -> thickness.otrAt(m, props.getPerforatedFallbackThicknessUm())).orElse(0.0);
            reasons.add("No sealable film breathes enough → " + bestName + " " + bestT + " µm with micro-perforations");
        } else {
            reasons.add("Best breathable film: " + bestName + " " + bestT + " µm (OTR " + Num.fmt(bestOtr)
                    + ", closest above the requirement)");
        }
        return new MapResult(target.orElse(null), rr, reqOtr, targetO2, bestName, bestT, bestOtr, perforated,
                List.copyOf(reasons), note);
    }
}
