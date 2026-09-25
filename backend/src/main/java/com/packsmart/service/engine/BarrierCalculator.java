package com.packsmart.service.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.packsmart.config.EngineProperties;
import com.packsmart.service.engine.model.BarrierRequirements;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.MoistureMode;
import com.packsmart.service.engine.model.O2Barrier;
import com.packsmart.service.engine.model.Requirements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Step B - required OTR and WVTR from physics. Pure. */
@Service
@RequiredArgsConstructor
public class BarrierCalculator {

    private final EngineProperties props;

    /** Q10 temperature factor: {@code tf(T, Tref) = q10 ^ ((T − Tref) / 10)}. */
    public double temperatureFactor(double tempC, double refTempC) {
        return Math.pow(props.getQ10(), (tempC - refTempC) / 10.0);
    }

    /** Flat pouch approximation: {@code area = areaBase + areaPerGram × packWeightG} (m²). */
    public double estimateArea(double packWeightG) {
        return props.getAreaBaseM2() + props.getAreaPerGramM2() * packWeightG;
    }

    /** {@code O2_tolerance_mL = o2ToleranceMlPerKg[level] × packWeightKg}. */
    public double o2ToleranceMl(O2Barrier level, double packWeightKg) {
        Double perKg = props.getO2ToleranceMlPerKg().get(level.name().toLowerCase(Locale.ROOT));
        if (perKg == null) {
            throw new IllegalStateException("engine.o2-tolerance-ml-per-kg has no value for " + level);
        }
        return perKg * packWeightKg;
    }

    /** {@code OTR_required = O2_tolerance_mL / (area × shelfLifeDays × pO2 × tf(T, 23))}. */
    public double requiredOtr(double o2ToleranceMl, double areaM2, double shelfLifeDays, double tempFactor) {
        return o2ToleranceMl / (areaM2 * shelfLifeDays * props.getO2PartialPressureAtm() * tempFactor);
    }

    /** {@code allowedWater_g = allowedMoistureGainFraction × packWeightG}. */
    public double allowedWaterG(double packWeightG) {
        return props.getAllowedMoistureGainFraction() * packWeightG;
    }

    /** {@code drivingFactor = |RH_outside − aw × 100| / 90}. */
    public double drivingFactor(double rhPct, double waterActivity) {
        return Math.abs(rhPct - waterActivity * 100.0) / props.getDrivingFactorDivisor();
    }

    /** {@code WVTR_required = allowedWater_g / (area × shelfLifeDays × drivingFactor × tf(T, 38))}. */
    public double requiredWvtr(double allowedWaterG, double areaM2, double shelfLifeDays, double drivingFactor, double tempFactor) {
        return allowedWaterG / (areaM2 * shelfLifeDays * drivingFactor * tempFactor);
    }

    /**
     * Computes the maximum OTR and WVTR the pack may have.
     * OTR is skipped when the oxygen barrier is LOW; WVTR only applies to KEEP_OUT/KEEP_IN and when drivingFactor ≥ minDrivingFactor.
     */
    public BarrierRequirements compute(FoodProfile food, Conditions cond, Requirements req) {
        List<String> reasons = new ArrayList<>();
        double area = cond.areaM2();
        int days = cond.shelfLifeDays();
        double tfO2 = temperatureFactor(cond.storageTempC(), props.getReferenceOtrTempC());
        double tfH2O = temperatureFactor(cond.storageTempC(), props.getReferenceWvtrTempC());

        Double reqOtr = null;
        double tolerance = 0;
        if (req.o2Barrier() != O2Barrier.LOW) {
            tolerance = o2ToleranceMl(req.o2Barrier(), cond.packWeightKg());
            reqOtr = requiredOtr(tolerance, area, days, tfO2);
            reasons.add("Food can absorb only " + Num.fmt(tolerance) + " mL O₂ (" + req.o2Barrier() + " sensitivity × "
                    + Num.fmt(cond.packWeightKg()) + " kg) → required OTR ≤ " + Num.fmt(reqOtr)
                    + " cc/m²·day·atm (area " + Num.fmt(area) + " m², " + days + " days, temp factor " + Num.fmt(tfO2) + ")");
        }

        Double reqWvtr = null;
        double allowedWater = allowedWaterG(cond.packWeightG());
        double driving = drivingFactor(cond.relativeHumidityPct(), food.waterActivity());
        if (req.moistureMode() == MoistureMode.KEEP_OUT || req.moistureMode() == MoistureMode.KEEP_IN) {
            if (driving < props.getMinDrivingFactor()) {
                reasons.add("Outside RH " + Num.fmt(cond.relativeHumidityPct()) + "% is almost equal to the food's aw → "
                        + "no moisture driving force, no WVTR limit");
            } else {
                reqWvtr = requiredWvtr(allowedWater, area, days, driving, tfH2O);
                String verb = req.moistureMode() == MoistureMode.KEEP_OUT ? "gain" : "lose";
                reasons.add("Food may " + verb + " at most " + Num.fmt(allowedWater) + " g water ("
                        + Num.fmt(props.getAllowedMoistureGainFraction() * 100) + "% of pack) → required WVTR ≤ "
                        + Num.fmt(reqWvtr) + " g/m²·day (humidity driving factor " + Num.fmt(driving)
                        + ", temp factor " + Num.fmt(tfH2O) + ")");
            }
        }
        return new BarrierRequirements(reqOtr, reqWvtr, tolerance, allowedWater, driving, tfO2, tfH2O, List.copyOf(reasons));
    }
}
