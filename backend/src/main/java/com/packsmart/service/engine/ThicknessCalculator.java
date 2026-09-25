package com.packsmart.service.engine;

import java.util.List;

import com.packsmart.config.EngineProperties;
import com.packsmart.service.engine.model.MaterialData;
import com.packsmart.service.engine.model.ThicknessChoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Step D - thinnest standard thickness of a single film that meets the barrier requirements. Pure. */
@Service
@RequiredArgsConstructor
public class ThicknessCalculator {

    private final EngineProperties props;
    private final LaminateService laminates;

    /** Standard thicknesses between min and max thickness, ascending. */
    public List<Integer> allowedThicknesses() {
        return props.getThicknessOptionsUm().stream()
                .filter(t -> t >= props.getMinThicknessUm() && t <= props.getMaxThicknessUm())
                .sorted()
                .toList();
    }

    /** {@code OTR(t) = OTR_25 × 25 / t}. */
    public double otrAt(MaterialData m, double thicknessUm) {
        return laminates.layerTransmission(m.otr25(), thicknessUm);
    }

    /** {@code WVTR(t) = WVTR_25 × 25 / t}. */
    public double wvtrAt(MaterialData m, double thicknessUm) {
        return laminates.layerTransmission(m.wvtr25(), thicknessUm);
    }

    /**
     * Smallest allowed thickness with {@code OTR(t) ≤ requiredOtr} and {@code WVTR(t) ≤ requiredWvtr}
     * (a null requirement is ignored). If none works the max thickness is returned with passes = false.
     */
    public ThicknessChoice choose(MaterialData m, Double requiredOtr, Double requiredWvtr) {
        List<Integer> options = allowedThicknesses();
        for (int t : options) {
            boolean otrOk = requiredOtr == null || otrAt(m, t) <= requiredOtr;
            boolean wvtrOk = requiredWvtr == null || wvtrAt(m, t) <= requiredWvtr;
            if (otrOk && wvtrOk) {
                String why = requiredOtr == null && requiredWvtr == null
                        ? t + " µm is the thinnest standard gauge (no barrier limit)"
                        : t + " µm is the thinnest gauge meeting the barrier limits";
                return new ThicknessChoice(t, true, why);
            }
        }
        int max = options.get(options.size() - 1);
        StringBuilder why = new StringBuilder("even at " + max + " µm");
        if (requiredOtr != null && otrAt(m, max) > requiredOtr) {
            why.append(" OTR is ").append(Num.fmt(otrAt(m, max))).append(" > required ").append(Num.fmt(requiredOtr));
        }
        if (requiredWvtr != null && wvtrAt(m, max) > requiredWvtr) {
            if (why.toString().contains("OTR")) {
                why.append(" and");
            }
            why.append(" WVTR is ").append(Num.fmt(wvtrAt(m, max))).append(" > required ").append(Num.fmt(requiredWvtr));
        }
        return new ThicknessChoice(max, false, why.toString());
    }
}
