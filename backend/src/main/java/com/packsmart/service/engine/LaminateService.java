package com.packsmart.service.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.packsmart.config.EngineProperties;
import com.packsmart.service.engine.model.LaminateEval;
import com.packsmart.service.engine.model.Layer;
import com.packsmart.service.engine.model.MaterialData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Step C - combines layers into one structure. Pure. */
@Service
@RequiredArgsConstructor
public class LaminateService {

    private final EngineProperties props;

    /** Transmission of one layer at thickness t: {@code value_25 × 25 / t}. */
    public double layerTransmission(double valueAt25um, double thicknessUm) {
        return valueAt25um * props.getReferenceThicknessUm() / thicknessUm;
    }

    /**
     * Layers in series: {@code 1 / total = Σ 1 / layer}.
     * A layer with zero transmission (e.g. glass) makes the whole stack a perfect barrier (total 0).
     */
    public double seriesTotal(List<Double> layerValues) {
        double inverseSum = 0;
        for (double v : layerValues) {
            if (v <= 0) {
                return 0;
            }
            inverseSum += 1.0 / v;
        }
        return 1.0 / inverseSum;
    }

    /**
     * Evaluates a stack (outside → inside): OTR/WVTR in series; min temp = max of layers; max temp = min of layers;
     * transparent = all layers; heat sealable = innermost layer; strength = max; biodegradable = all;
     * recyclable = all layers share one family and every layer is recyclable (mono-material), else family "mixed".
     */
    public LaminateEval evaluate(List<Layer> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("A structure needs at least one layer");
        }
        List<Double> otrs = new ArrayList<>();
        List<Double> wvtrs = new ArrayList<>();
        double total = 0;
        double minTemp = Double.NEGATIVE_INFINITY;
        double maxTemp = Double.POSITIVE_INFINITY;
        boolean transparent = true;
        boolean biodegradable = true;
        boolean allRecyclable = true;
        boolean containsAl = false;
        boolean approx = false;
        int strength = 0;
        String family = null;
        boolean sameFamily = true;
        for (Layer layer : layers) {
            MaterialData m = layer.material();
            if (layer.thicknessUm() <= 0) {
                throw new IllegalArgumentException("Thickness of " + m.name() + " must be > 0 µm");
            }
            otrs.add(layerTransmission(m.otr25(), layer.thicknessUm()));
            wvtrs.add(layerTransmission(m.wvtr25(), layer.thicknessUm()));
            total += layer.thicknessUm();
            minTemp = Math.max(minTemp, m.minTempC());
            maxTemp = Math.min(maxTemp, m.maxTempC());
            transparent &= m.transparent();
            biodegradable &= m.biodegradable();
            allRecyclable &= m.recyclable();
            approx |= m.approx();
            strength = Math.max(strength, m.strength());
            containsAl |= props.getEcoScore().getAluminiumFamily().equalsIgnoreCase(m.family());
            if (family == null) {
                family = m.family();
            } else if (!Objects.equals(family, m.family())) {
                sameFamily = false;
            }
        }
        boolean mono = sameFamily && family != null;
        boolean heatSealable = layers.get(layers.size() - 1).material().heatSealable();
        return new LaminateEval(seriesTotal(otrs), seriesTotal(wvtrs), total, minTemp, maxTemp, transparent, heatSealable,
                strength, biodegradable, mono && allRecyclable, mono ? family : LaminateEval.MIXED, containsAl, approx);
    }

    /**
     * Parses {@code "PET:12;Aluminium foil:9;LDPE:50"}: split on ';' then on the LAST ':'
     * (material names may contain spaces and brackets).
     */
    public List<Layer> parseLayers(String spec, Function<String, Optional<MaterialData>> lookup) {
        List<Layer> layers = new ArrayList<>();
        for (String part : spec.split(";")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int idx = p.lastIndexOf(':');
            if (idx <= 0) {
                throw new IllegalArgumentException("Bad layer '" + p + "' (expected Material:thickness)");
            }
            String name = p.substring(0, idx).trim();
            double thickness = Double.parseDouble(p.substring(idx + 1).trim());
            MaterialData m = lookup.apply(name)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown material '" + name + "' in layers"));
            layers.add(new Layer(m, thickness));
        }
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("No layers in '" + spec + "'");
        }
        return layers;
    }
}
