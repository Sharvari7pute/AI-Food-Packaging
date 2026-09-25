package com.packsmart.engine;

import static com.packsmart.engine.TestEngine.MAP;
import static com.packsmart.engine.TestEngine.SHELF_LIFE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import com.packsmart.service.engine.model.CurvePoint;
import org.junit.jupiter.api.Test;

class ShelfLifeAndMapTest {

    @Test
    void oxygenShelfLifeInvertsRequiredOtr() {
        // If OTR equals the required OTR, shelf life equals the requested days
        double tol = 0.1;
        double area = 0.06;
        double tf = 1.5;
        double reqOtr = tol / (area * 90 * 0.21 * tf);
        assertThat(SHELF_LIFE.oxygenDays(tol, reqOtr, area, tf)).isCloseTo(90, within(1e-9));
    }

    @Test
    void perfectBarrierIsInfinite() {
        assertThat(SHELF_LIFE.oxygenDays(1, 0, 0.06, 1)).isInfinite();
    }

    @Test
    void curveHasAtMost60PointsAndReaches100PercentAtShelfLife() {
        List<CurvePoint> curve = SHELF_LIFE.curve(100.0, null, 100);
        assertThat(curve).hasSizeLessThanOrEqualTo(60);
        assertThat(curve.get(0).o2UsedPct()).isZero();
        assertThat(curve.get(0).moistureUsedPct()).isNull();
        assertThat(curve.get(curve.size() - 1).day()).isLessThanOrEqualTo(150);
        CurvePoint atLife = curve.stream().filter(p -> p.day() >= 100).findFirst().orElseThrow();
        assertThat(atLife.o2UsedPct()).isGreaterThanOrEqualTo(100);
    }

    @Test
    void respirationAndMapOtr() {
        double rr = MAP.respirationMlPerDay(15, 0.5, 20);
        assertThat(rr).isCloseTo(180, within(1e-9));
        double otr = MAP.requiredOtr(rr, 0.22, 4);
        assertThat(otr).isCloseTo(180 / (0.22 * 0.17), within(1e-6));
    }
}
