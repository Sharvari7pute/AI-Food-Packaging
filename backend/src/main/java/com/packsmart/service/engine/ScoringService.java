package com.packsmart.service.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.packsmart.config.EngineProperties;
import com.packsmart.service.engine.model.BarrierRequirements;
import com.packsmart.service.engine.model.Candidate;
import com.packsmart.service.engine.model.CandidateKind;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.LaminateEval;
import com.packsmart.service.engine.model.Priority;
import com.packsmart.service.engine.model.Requirements;
import com.packsmart.service.engine.model.Scores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Step F - hard filters, 0-1 scores and ranking. Pure. */
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final EngineProperties props;
    private final TopsisRanker topsis;

    /**
     * Hard filters. Returns the reasons a candidate is rejected (empty = passes).
     * Barrier (when {@code checkBarrier}): OTR ≤ requiredOtr and WVTR ≤ requiredWvtr, or for MAP OTR ≥ mapRequiredOtr.
     * Temperature: min_temp ≤ storage temp (and ≤ −25 °C when frozen), max_temp ≥ storage temp.
     * Opaque if required; strength ≥ 3 for long distance; innermost layer heat sealable.
     */
    public List<String> hardFilters(Candidate c, Requirements req, BarrierRequirements b, Conditions cond,
                                    Double mapRequiredOtr, boolean checkBarrier) {
        List<String> fails = new ArrayList<>();
        LaminateEval e = c.getEval();
        if (checkBarrier) {
            if (mapRequiredOtr != null) {
                if (e.otr() < mapRequiredOtr) {
                    fails.add("OTR " + Num.fmt(e.otr()) + " < required " + Num.fmt(mapRequiredOtr) + " → not breathable enough for produce");
                }
            } else {
                if (b.hasOtr() && e.otr() > b.requiredOtr()) {
                    fails.add("OTR " + Num.fmt(e.otr()) + " > required " + Num.fmt(b.requiredOtr()) + " → too much oxygen gets in");
                }
                if (b.hasWvtr() && e.wvtr() > b.requiredWvtr()) {
                    fails.add("WVTR " + Num.fmt(e.wvtr()) + " > required " + Num.fmt(b.requiredWvtr()) + " → too much moisture passes");
                }
            }
        }
        double t = cond.storageTempC();
        if (e.minTempC() > t) {
            fails.add("Min use temperature " + Num.fmt(e.minTempC()) + " °C is above storage " + Num.fmt(t) + " °C (becomes brittle)");
        } else if (req.frozen() && e.minTempC() > props.getFrozenMinTempC()) {
            fails.add("Min use temperature " + Num.fmt(e.minTempC()) + " °C not suitable for frozen storage (needs ≤ "
                    + Num.fmt(props.getFrozenMinTempC()) + " °C)");
        }
        if (e.maxTempC() < t) {
            fails.add("Max use temperature " + Num.fmt(e.maxTempC()) + " °C is below storage " + Num.fmt(t) + " °C");
        }
        if (req.opaque() && e.transparent()) {
            fails.add("Transparent, but this food needs an opaque (light-blocking) pack");
        }
        if (req.needsStrength() && e.strength() < props.getMinStrengthLongDistance()) {
            fails.add("Strength " + e.strength() + "/" + props.getStrengthScale() + " too low for long-distance transport");
        }
        if (!e.heatSealable()) {
            fails.add(c.getKind() == CandidateKind.FILM
                    ? "Not heat sealable on its own → cannot close the pack"
                    : "Innermost layer is not heat sealable → cannot close the pack");
        }
        return fails;
    }

    /**
     * Barrier margin = the tighter (smaller) of requiredOtr/OTR and requiredWvtr/WVTR. Infinite for a perfect barrier;
     * null when there is no barrier requirement.
     */
    public Double barrierMargin(LaminateEval e, BarrierRequirements b) {
        Double margin = null;
        if (b.hasOtr()) {
            margin = ratio(b.requiredOtr(), e.otr());
        }
        if (b.hasWvtr()) {
            double m = ratio(b.requiredWvtr(), e.wvtr());
            margin = margin == null ? m : Math.min(margin, m);
        }
        return margin;
    }

    private static double ratio(double required, double actual) {
        return actual <= 0 ? Double.POSITIVE_INFINITY : required / actual;
    }

    /** Eco score: biodegradable 1.0, recyclable mono-material 0.8, contains aluminium 0.1, else 0.3. */
    public double ecoScore(LaminateEval e) {
        EngineProperties.EcoScore eco = props.getEcoScore();
        if (e.biodegradable()) {
            return eco.getBiodegradable();
        }
        if (e.recyclable()) {
            return eco.getRecyclableMono();
        }
        if (e.containsAluminium()) {
            return eco.getContainsAluminium();
        }
        return eco.getOther();
    }

    /**
     * Scores (0-1): barrier = 1.0 if margin ≤ overkillMargin else 0.8 (for MAP: requiredOtr/OTR, closeness);
     * cost = cheapest passing cost per pack / this cost per pack; eco as {@link #ecoScore}; strength = strength / 5.
     * total = weighted sum with the priority's weights.
     */
    public Scores score(Candidate c, double minCostPerPack, BarrierRequirements b, Priority priority, Double mapRequiredOtr) {
        LaminateEval e = c.getEval();
        double barrierScore;
        if (mapRequiredOtr != null) {
            barrierScore = e.otr() <= 0 ? 0 : Math.min(1.0, mapRequiredOtr / e.otr());
        } else {
            Double margin = barrierMargin(e, b);
            barrierScore = margin == null || margin <= props.getOverkillMargin()
                    ? props.getBarrierScore().getWithinMargin()
                    : props.getBarrierScore().getOverkill();
        }
        double cost = c.getEconomics().costPerPackInr();
        double costScore = cost <= 0 ? 1.0 : Math.min(1.0, minCostPerPack / cost);
        double ecoScore = ecoScore(e);
        double strengthScore = Math.min(1.0, e.strength() / (double) props.getStrengthScale());
        EngineProperties.Weights w = props.weightsFor(priority.name());
        double total = w.getBarrier() * barrierScore + w.getCost() * costScore + w.getEco() * ecoScore
                + w.getStrength() * strengthScore;
        return new Scores(barrierScore, costScore, ecoScore, strengthScore, total);
    }

    /**
     * Ranks passing candidates. Non-MAP: TOPSIS over four criteria (barrier safety margin, cost per pack, eco score,
     * strength) with the priority's weights - or the plain weighted total when {@code engine.ranking-method=weighted}.
     * MAP: closeness to the required OTR.
     */
    public List<Candidate> rank(List<Candidate> passed, boolean map, BarrierRequirements b, Priority priority) {
        if (!map && "topsis".equalsIgnoreCase(props.getRankingMethod()) && !passed.isEmpty()) {
            applyTopsis(passed, b, priority);
            return passed.stream()
                    .sorted(Comparator.comparingDouble((Candidate c) -> -c.getScores().topsis())
                            .thenComparingDouble(c -> c.getEconomics().costPerPackInr())
                            .thenComparing(Candidate::getName))
                    .toList();
        }
        return rank(passed, map);
    }

    /**
     * TOPSIS criteria per candidate: barrier = log10(min(margin, overkillMargin)) (benefit - extra barrier beyond the
     * overkill margin earns nothing), cost per pack (cost), eco score (benefit), strength (benefit).
     */
    void applyTopsis(List<Candidate> passed, BarrierRequirements b, Priority priority) {
        EngineProperties.Weights w = props.weightsFor(priority.name());
        List<TopsisRanker.Criterion> criteria = List.of(
                new TopsisRanker.Criterion("barrier", w.getBarrier(), true),
                new TopsisRanker.Criterion("cost", w.getCost(), false),
                new TopsisRanker.Criterion("eco", w.getEco(), true),
                new TopsisRanker.Criterion("strength", w.getStrength(), true));
        List<double[]> matrix = new java.util.ArrayList<>();
        for (Candidate c : passed) {
            Double margin = barrierMargin(c.getEval(), b);
            double capped = margin == null ? 1 : Math.min(Math.max(margin, 1), props.getOverkillMargin());
            // log10(1) = 0 would make every "just passes" pack identical on this axis; shift by 1 to keep it positive
            double barrier = 1 + Math.log10(capped);
            matrix.add(new double[]{barrier, c.getEconomics().costPerPackInr(), ecoScore(c.getEval()), c.getEval().strength()});
        }
        List<Double> cc = topsis.closeness(matrix, criteria);
        for (int i = 0; i < passed.size(); i++) {
            Candidate c = passed.get(i);
            c.setScores(c.getScores().withTopsis(cc.get(i)));
        }
    }

    /** Ranks passing candidates: by total score (then cheaper first); for MAP by closeness to the required OTR. */
    public List<Candidate> rank(List<Candidate> passed, boolean map) {
        Comparator<Candidate> byScore = Comparator.comparingDouble((Candidate c) -> -c.getScores().total())
                .thenComparingDouble(c -> c.getEconomics().costPerPackInr())
                .thenComparing(Candidate::getName);
        Comparator<Candidate> order = map
                ? Comparator.comparingDouble((Candidate c) -> c.getMapCloseness() == null ? Double.MAX_VALUE : c.getMapCloseness())
                        .thenComparing(byScore)
                : byScore;
        return passed.stream().sorted(order).toList();
    }
}
