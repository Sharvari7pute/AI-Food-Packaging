package com.packsmart.service.engine;

import java.util.ArrayList;
import java.util.List;

import com.packsmart.config.EngineProperties;
import com.packsmart.service.engine.model.BarrierRequirements;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.CurvePoint;
import com.packsmart.service.engine.model.LaminateEval;
import com.packsmart.service.engine.model.LimitingFactor;
import com.packsmart.service.engine.model.ShelfLife;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Step G - how long a structure protects the food, and the shelf-life curve. Pure. */
@Service
@RequiredArgsConstructor
public class ShelfLifeService {

    private static final double PERCENT = 100.0;

    private final EngineProperties props;

    /** {@code shelfLife_O2 = O2_tolerance_mL / (OTR × area × pO2 × tf(T, 23))}; infinite for a perfect barrier. */
    public double oxygenDays(double o2ToleranceMl, double otr, double areaM2, double tempFactor) {
        double rate = otr * areaM2 * props.getO2PartialPressureAtm() * tempFactor;
        return rate <= 0 ? Double.POSITIVE_INFINITY : o2ToleranceMl / rate;
    }

    /** {@code shelfLife_H2O = allowedWater_g / (WVTR × area × drivingFactor × tf(T, 38))}; infinite for a perfect barrier. */
    public double moistureDays(double allowedWaterG, double wvtr, double areaM2, double drivingFactor, double tempFactor) {
        double rate = wvtr * areaM2 * drivingFactor * tempFactor;
        return rate <= 0 ? Double.POSITIVE_INFINITY : allowedWaterG / rate;
    }

    /**
     * {@code estimated = min(applicable mechanisms)} capped at maxShelfLifeDays; a mechanism applies when Step B set a
     * requirement for it. If nothing applies the commodity default is used and limitingFactor is NONE.
     */
    public ShelfLife estimate(LaminateEval eval, BarrierRequirements b, Conditions cond, Integer defaultShelfLifeDays) {
        Double o2Days = b.hasOtr() ? oxygenDays(b.o2ToleranceMl(), eval.otr(), cond.areaM2(), b.otrTempFactor()) : null;
        Double h2oDays = b.hasWvtr()
                ? moistureDays(b.allowedWaterG(), eval.wvtr(), cond.areaM2(), b.drivingFactor(), b.wvtrTempFactor())
                : null;

        int estimated;
        LimitingFactor factor;
        if (o2Days == null && h2oDays == null) {
            estimated = defaultShelfLifeDays != null ? defaultShelfLifeDays : cond.shelfLifeDays();
            factor = LimitingFactor.NONE;
        } else {
            double limit;
            if (h2oDays == null || (o2Days != null && o2Days <= h2oDays)) {
                limit = o2Days;
                factor = LimitingFactor.OXYGEN;
            } else {
                limit = h2oDays;
                factor = LimitingFactor.MOISTURE;
            }
            estimated = (int) Math.floor(Math.min(limit, props.getMaxShelfLifeDays()));
        }
        return new ShelfLife(o2Days, h2oDays, estimated, factor, curve(o2Days, h2oDays, estimated));
    }

    /**
     * Curve points from day 0 to ceil(curveExtension × estimated), at most curveMaxPoints:
     * {@code o2UsedPct = 100 × day / shelfLife_O2}, {@code moistureUsedPct = 100 × day / shelfLife_H2O}.
     */
    public List<CurvePoint> curve(Double o2Days, Double h2oDays, int estimatedDays) {
        int maxDay = Math.max(1, (int) Math.ceil(props.getCurveExtension() * estimatedDays));
        int step = Math.max(1, (int) Math.ceil(maxDay / (double) (props.getCurveMaxPoints() - 1)));
        List<CurvePoint> points = new ArrayList<>();
        for (int day = 0; day <= maxDay && points.size() < props.getCurveMaxPoints(); day += step) {
            points.add(new CurvePoint(day, usedPct(day, o2Days), usedPct(day, h2oDays)));
        }
        return points;
    }

    private static Double usedPct(int day, Double lifeDays) {
        if (lifeDays == null) {
            return null;
        }
        if (Double.isInfinite(lifeDays) || lifeDays <= 0) {
            return lifeDays <= 0 ? PERCENT : 0.0;
        }
        return Num.dp(PERCENT * day / lifeDays, 2);
    }
}
