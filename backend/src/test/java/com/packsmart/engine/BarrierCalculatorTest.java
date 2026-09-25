package com.packsmart.engine;

import static com.packsmart.engine.TestEngine.BARRIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.packsmart.service.engine.model.BarrierRequirements;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.O2Barrier;
import com.packsmart.service.engine.model.StorageType;
import org.junit.jupiter.api.Test;

class BarrierCalculatorTest {

    @Test
    void q10FactorDoublesPerTenDegrees() {
        assertThat(BARRIER.temperatureFactor(33, 23)).isEqualTo(2.0);
        assertThat(BARRIER.temperatureFactor(23, 23)).isEqualTo(1.0);
        assertThat(BARRIER.temperatureFactor(13, 23)).isEqualTo(0.5);
    }

    @Test
    void areaEstimateIsFlatPouchApproximation() {
        assertThat(BARRIER.estimateArea(100)).isCloseTo(0.06, within(1e-12));
    }

    @Test
    void requiredOtrForChips() {
        double tolerance = BARRIER.o2ToleranceMl(O2Barrier.HIGH, 0.1);
        assertThat(tolerance).isCloseTo(0.1, within(1e-12));
        double tf = BARRIER.temperatureFactor(30, 23);
        double otr = BARRIER.requiredOtr(tolerance, 0.06, 90, tf);
        assertThat(otr).isCloseTo(0.1 / (0.06 * 90 * 0.21 * Math.pow(2, 0.7)), within(1e-12));
    }

    @Test
    void drivingFactorAndWvtr() {
        assertThat(BARRIER.drivingFactor(70, 0.2)).isCloseTo(50.0 / 90, within(1e-12));
        double wvtr = BARRIER.requiredWvtr(2, 0.06, 90, 50.0 / 90, 1);
        assertThat(wvtr).isCloseTo(2 / (0.06 * 90 * 50.0 / 90), within(1e-12));
    }

    @Test
    void noWvtrRequirementWhenHumidityMatchesWaterActivity() {
        FoodProfile paneer = TestEngine.food("Paneer");
        Conditions c = TestEngine.conditions(200, 7, StorageType.CHILLED, 4, 96);
        var req = TestEngine.REQUIREMENTS.derive(paneer, c);
        BarrierRequirements b = BARRIER.compute(paneer, c, req);
        assertThat(b.requiredWvtr()).isNull();
        assertThat(b.requiredOtr()).isNotNull();
    }
}
