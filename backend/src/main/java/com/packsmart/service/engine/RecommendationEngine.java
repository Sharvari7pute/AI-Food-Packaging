package com.packsmart.service.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.packsmart.config.EngineProperties;
import com.packsmart.dto.CustomCommodity;
import com.packsmart.dto.Overrides;
import com.packsmart.dto.RecommendRequest;
import com.packsmart.entity.Commodity;
import com.packsmart.service.CatalogMapper;
import com.packsmart.service.CatalogService;
import com.packsmart.service.engine.model.BarrierRequirements;
import com.packsmart.service.engine.model.Candidate;
import com.packsmart.service.engine.model.CandidateKind;
import com.packsmart.service.engine.model.Catalog;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.EngineResult;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.LaminateDef;
import com.packsmart.service.engine.model.Layer;
import com.packsmart.service.engine.model.MapResult;
import com.packsmart.service.engine.model.MaterialData;
import com.packsmart.service.engine.model.Requirements;
import com.packsmart.service.engine.model.Scores;
import com.packsmart.service.engine.model.ThicknessChoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates Steps A → H. {@link #compute} is pure (all data passed in via {@link Catalog});
 * persistence of results lives in {@link com.packsmart.service.RecommendationService}.
 */
@Service
@RequiredArgsConstructor
public class RecommendationEngine {

    private final EngineProperties props;
    private final RequirementService requirements;
    private final BarrierCalculator barrier;
    private final LaminateService laminates;
    private final ThicknessCalculator thickness;
    private final PackEconomics economics;
    private final ScoringService scoring;
    private final ShelfLifeService shelfLife;
    private final MapCalculator mapCalculator;
    private final CatalogService catalogService;

    // ------------------------------------------------------------------ request resolution

    /** Commodity from the DB (or the custom food) with non-null overrides applied. */
    public FoodProfile resolveFood(RecommendRequest req) {
        FoodProfile base;
        if (req.commodityId() != null) {
            Commodity c = catalogService.commodityEntity(req.commodityId());
            base = CatalogMapper.toFood(c);
        } else {
            base = fromCustom(req.customCommodity());
        }
        return applyOverrides(base, req.overrides());
    }

    public static FoodProfile fromCustom(CustomCommodity c) {
        return new FoodProfile(null, c.name().trim(), c.nameHi(), c.category(), c.moisturePct(), c.waterActivity(),
                c.criticalAw(), c.fatPct(), c.o2Sensitive() != null ? c.o2Sensitive() : "medium",
                c.lightSensitive() != null ? c.lightSensitive() : "medium", Boolean.TRUE.equals(c.respiring()),
                c.respirationRate(), c.defaultShelfLifeDays(), Boolean.TRUE.equals(c.aiEstimated()));
    }

    public static FoodProfile applyOverrides(FoodProfile f, Overrides o) {
        if (o == null) {
            return f;
        }
        return new FoodProfile(f.commodityId(), f.name(), f.nameHi(), f.category(),
                o.moisturePct() != null ? o.moisturePct() : f.moisturePct(),
                o.waterActivity() != null ? o.waterActivity() : f.waterActivity(),
                f.criticalAw(),
                o.fatPct() != null ? o.fatPct() : f.fatPct(),
                f.o2Sensitive(), f.lightSensitive(), f.respiring(),
                o.respirationRate() != null ? o.respirationRate() : f.respirationRate(),
                f.defaultShelfLifeDays(), f.aiEstimated());
    }

    /** Conditions with the pack area resolved ({@code area = 0.02 + 0.0004 × packWeightG} when not given). */
    public Conditions resolveConditions(RecommendRequest req) {
        boolean estimated = req.packAreaM2() == null;
        double area = estimated ? barrier.estimateArea(req.packWeightG()) : req.packAreaM2();
        return new Conditions(req.packWeightG(), area, estimated, req.shelfLifeDays(), req.storageType(),
                req.storageTempC(), req.relativeHumidityPct(), req.transportOrDefault(), req.priorityOrDefault());
    }

    // ------------------------------------------------------------------ pipeline

    /** Runs Steps A → H for one food and set of conditions. Pure: no DB or network access. */
    public EngineResult compute(FoodProfile food, Conditions cond, Catalog catalog) {
        Requirements req = requirements.derive(food, cond);

        MapResult map = null;
        BarrierRequirements b;
        Double mapOtr = null;
        if (req.needsMap()) {
            map = mapCalculator.compute(food, cond, catalog.materials(), catalog.mapTarget(food.name()));
            mapOtr = map.requiredOtr();
            b = BarrierRequirements.none(map.reasons());
        } else {
            b = barrier.compute(food, cond, req);
        }

        List<Candidate> all = new ArrayList<>();
        for (MaterialData m : catalog.materials()) {
            if (m.isRigid()) {
                continue;
            }
            ThicknessChoice tc = req.needsMap()
                    ? mapCalculator.chooseThickness(m, mapOtr)
                    : thickness.choose(m, b.requiredOtr(), b.requiredWvtr());
            Candidate c = candidate(m.name(), CandidateKind.FILM, List.of(new Layer(m, tc.thicknessUm())), cond);
            if (tc.passes()) {
                c.getReasons().add(tc.reason());
            } else {
                c.getFailReasons().add(tc.reason());
            }
            c.getFailReasons().addAll(scoring.hardFilters(c, req, b, cond, mapOtr, false));
            all.add(c);
        }
        for (LaminateDef l : catalog.laminates()) {
            Candidate c = candidate(l.name(), CandidateKind.LAMINATE, l.layers(), cond);
            if (l.typicalUse() != null) {
                c.getReasons().add("Typical use: " + l.typicalUse());
            }
            c.getFailReasons().addAll(scoring.hardFilters(c, req, b, cond, mapOtr, true));
            all.add(c);
        }
        for (Candidate c : all) {
            c.setShelfLife(shelfLife.estimate(c.getEval(), b, cond, food.defaultShelfLifeDays()));
            if (mapOtr != null && c.getEval().otr() > 0) {
                c.setMapCloseness(c.getEval().otr() / mapOtr);
            }
        }

        List<Candidate> passed = all.stream().filter(Candidate::passed).toList();
        double minCost = (passed.isEmpty() ? all : passed).stream()
                .mapToDouble(c -> c.getEconomics().costPerPackInr()).min().orElse(0);
        for (Candidate c : all) {
            c.setScores(scoring.score(c, minCost, b, cond.priority(), mapOtr));
        }

        List<Candidate> options = new ArrayList<>(scoring.rank(passed, req.needsMap()).stream()
                .limit(props.getTopOptions()).toList());
        if (req.needsMap() && options.isEmpty()) {
            perforatedFallback(catalog, cond, b, food, minCost, mapOtr).ifPresent(options::add);
        }
        options.forEach(c -> c.getReasons().add(0, whyChosen(c, b, req)));

        Candidate avoid = avoid(all, catalog, req, b, cond, mapOtr, food);
        List<Candidate> nearMisses = options.size() >= props.getTopOptions() ? List.of() : nearMisses(all, req.needsMap());
        return new EngineResult(food, cond, req, b, List.copyOf(options), nearMisses, avoid, map);
    }

    private Candidate candidate(String name, CandidateKind kind, List<Layer> layers, Conditions cond) {
        return new Candidate(name, kind, layers, laminates.evaluate(layers), economics.compute(layers, cond.areaM2()));
    }

    private String whyChosen(Candidate c, BarrierRequirements b, Requirements req) {
        if (c.isPerforationNeeded()) {
            return "Micro-perforated film lets the produce breathe when no plain film is permeable enough";
        }
        if (req.needsMap()) {
            return "OTR " + Num.fmt(c.getEval().otr()) + " is closest above the MAP requirement";
        }
        Double margin = scoring.barrierMargin(c.getEval(), b);
        if (margin == null) {
            return "No barrier limit for this food, chosen on cost, eco and strength";
        }
        return Double.isInfinite(margin) ? "Practically perfect barrier" : "Meets the barrier limits with a safety margin of ×" + Num.fmt(margin);
    }

    private Optional<Candidate> perforatedFallback(Catalog catalog, Conditions cond, BarrierRequirements b, FoodProfile food,
                                                   double minCost, Double mapOtr) {
        return catalog.material(props.getPerforatedFallbackMaterial()).map(m -> {
            Candidate c = candidate(m.name() + " (micro-perforated)", CandidateKind.FILM,
                    List.of(new Layer(m, props.getPerforatedFallbackThicknessUm())), cond);
            c.setPerforationNeeded(true);
            c.setShelfLife(shelfLife.estimate(c.getEval(), b, cond, food.defaultShelfLifeDays()));
            c.setScores(scoring.score(c, minCost, b, cond.priority(), null));
            c.setMapCloseness(1.0);
            return c;
        });
    }

    /** Plain LDPE 50 µm if it fails; otherwise the lowest-scoring failed candidate. */
    private Candidate avoid(List<Candidate> all, Catalog catalog, Requirements req, BarrierRequirements b, Conditions cond,
                            Double mapOtr, FoodProfile food) {
        Optional<MaterialData> plain = catalog.material(props.getAvoidMaterial());
        if (plain.isPresent()) {
            Candidate c = candidate(plain.get().name(), CandidateKind.FILM,
                    List.of(new Layer(plain.get(), props.getAvoidThicknessUm())), cond);
            c.getFailReasons().addAll(scoring.hardFilters(c, req, b, cond, mapOtr, true));
            if (!c.passed()) {
                c.setShelfLife(shelfLife.estimate(c.getEval(), b, cond, food.defaultShelfLifeDays()));
                return c;
            }
        }
        return all.stream().filter(c -> !c.passed())
                .min(Comparator.comparingDouble((Candidate c) -> c.getScores().total()))
                .orElse(null);
    }

    /** Best failed candidates, by the shelf life they would reach (MAP: closest OTR). */
    private List<Candidate> nearMisses(List<Candidate> all, boolean map) {
        Comparator<Candidate> order = map
                ? Comparator.comparingDouble((Candidate c) -> c.getMapCloseness() == null ? 0 : -c.getMapCloseness())
                : Comparator.comparingInt((Candidate c) -> -c.getShelfLife().estimatedDays());
        return all.stream().filter(c -> !c.passed())
                .sorted(order.thenComparingDouble(c -> -c.getScores().total()))
                .limit(props.getNearMissCount())
                .toList();
    }

    // ------------------------------------------------------------------ Laminate Builder

    /** Evaluates a custom structure against a food's requirements (used by "test against a food"). */
    public Candidate testStructure(FoodProfile food, Conditions cond, List<Layer> layers, Catalog catalog) {
        Requirements req = requirements.derive(food, cond);
        Double mapOtr = null;
        BarrierRequirements b;
        if (req.needsMap()) {
            MapResult map = mapCalculator.compute(food, cond, catalog.materials(), catalog.mapTarget(food.name()));
            mapOtr = map.requiredOtr();
            b = BarrierRequirements.none(map.reasons());
        } else {
            b = barrier.compute(food, cond, req);
        }
        Candidate c = candidate("Custom", CandidateKind.LAMINATE, layers, cond);
        c.setBarrier(b);
        c.getFailReasons().addAll(scoring.hardFilters(c, req, b, cond, mapOtr, true));
        c.setShelfLife(shelfLife.estimate(c.getEval(), b, cond, food.defaultShelfLifeDays()));
        c.setScores(new Scores(0, 0, scoring.ecoScore(c.getEval()), 0, 0));
        c.getReasons().addAll(req.reasons());
        c.getReasons().addAll(b.reasons());
        if (mapOtr != null) {
            c.setMapCloseness(mapOtr);
        }
        return c;
    }
}
