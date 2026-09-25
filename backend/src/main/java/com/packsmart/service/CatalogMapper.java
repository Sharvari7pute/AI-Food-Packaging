package com.packsmart.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.packsmart.entity.Commodity;
import com.packsmart.entity.Laminate;
import com.packsmart.entity.MapTarget;
import com.packsmart.entity.Material;
import com.packsmart.entity.MaterialExtra;
import com.packsmart.service.engine.LaminateService;
import com.packsmart.service.engine.model.Catalog;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.LaminateDef;
import com.packsmart.service.engine.model.MapTargetData;
import com.packsmart.service.engine.model.MaterialData;
import lombok.extern.slf4j.Slf4j;

/** Converts database rows into the engine's pure model. Stateless, so tests can reuse it without a database. */
@Slf4j
public final class CatalogMapper {

    private CatalogMapper() {
    }

    /** A material note containing "approx" or "VERIFY" marks the row as approximate data. */
    public static boolean isApprox(String notes) {
        if (notes == null) {
            return false;
        }
        String n = notes.toLowerCase(Locale.ROOT);
        return n.contains("approx") || n.contains("verify");
    }

    public static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    /** Joins materials with extras (by exact trimmed name) and parses laminates. Incomplete rows are skipped with a warning. */
    public static Catalog toCatalog(List<Material> materials, List<MaterialExtra> extras, List<Laminate> laminates,
                                    List<MapTarget> mapTargets, LaminateService laminateService) {
        Map<String, MaterialExtra> extraByName = new HashMap<>();
        extras.forEach(e -> extraByName.put(key(e.getName()), e));

        List<MaterialData> data = new ArrayList<>();
        for (Material m : materials) {
            if (m.getOtr25um() == null || m.getWvtr25um() == null || m.getCostPerKgInr() == null
                    || m.getDensityGCm3() == null) {
                log.warn("Material '{}' is missing OTR/WVTR/cost/density - excluded from the engine", m.getName());
                continue;
            }
            MaterialExtra x = extraByName.get(key(m.getName()));
            data.add(new MaterialData(
                    m.getName().trim(),
                    m.getType(),
                    m.getOtr25um(),
                    m.getWvtr25um(),
                    m.getCostPerKgInr(),
                    m.getDensityGCm3(),
                    orDefault(m.getMinTempC(), -20),
                    orDefault(m.getMaxTempC(), 60),
                    Boolean.TRUE.equals(m.getRecyclable()),
                    Boolean.TRUE.equals(m.getBiodegradable()),
                    Boolean.TRUE.equals(m.getTransparent()),
                    Boolean.TRUE.equals(m.getHeatSealable()),
                    m.getStrength1to5() != null ? m.getStrength1to5() : 1,
                    x != null && x.getFamily() != null ? x.getFamily() : m.getName().trim().toUpperCase(Locale.ROOT),
                    x != null && x.getCo2eKgPerKg() != null ? x.getCo2eKgPerKg() : 0.0,
                    isApprox(m.getNotes()),
                    m.getSourceUrl(),
                    m.getNotes()));
        }
        Map<String, MaterialData> byName = new HashMap<>();
        data.forEach(d -> byName.put(key(d.name()), d));

        List<LaminateDef> lams = new ArrayList<>();
        for (Laminate l : laminates) {
            try {
                lams.add(new LaminateDef(l.getName(),
                        laminateService.parseLayers(l.getLayers(), n -> java.util.Optional.ofNullable(byName.get(key(n)))),
                        l.getTypicalUse()));
            } catch (RuntimeException e) {
                log.warn("Laminate '{}' skipped: {}", l.getName(), e.getMessage());
            }
        }
        Map<String, MapTargetData> targets = new HashMap<>();
        for (MapTarget t : mapTargets) {
            if (t.getTargetO2Min() == null || t.getTargetO2Max() == null) {
                log.warn("MAP target '{}' has no O2 range - skipped", t.getName());
                continue;
            }
            targets.put(key(t.getName()), new MapTargetData(t.getName(), t.getTargetO2Min(), t.getTargetO2Max(),
                    orDefault(t.getTargetCo2Min(), 0), orDefault(t.getTargetCo2Max(), 0), t.getStorageTempC()));
        }
        return new Catalog(List.copyOf(data), List.copyOf(lams), Map.copyOf(targets));
    }

    /** Commodity row → engine food profile (no overrides). */
    public static FoodProfile toFood(Commodity c) {
        if (c.getWaterActivity() == null) {
            throw new IllegalArgumentException("Commodity '" + c.getName() + "' has no water activity in the data");
        }
        return new FoodProfile(c.getId(), c.getName(), c.getNameHi(), c.getCategory(), c.getMoisturePct(),
                c.getWaterActivity(), c.getCriticalAw(), orDefault(c.getFatPct(), 0), c.getO2Sensitive(),
                c.getLightSensitive(), Boolean.TRUE.equals(c.getRespiring()), c.getRespirationRate(),
                c.getDefaultShelfLifeDays(), false, c.getPh(), c.getStorageTempMinC(), c.getStorageTempMaxC(),
                c.getMainDeteriorationFactor());
    }

    private static double orDefault(Double v, double d) {
        return v != null ? v : d;
    }
}
