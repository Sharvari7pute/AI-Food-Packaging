package com.packsmart.engine;

import static com.packsmart.engine.TestEngine.CATALOG;
import static com.packsmart.engine.TestEngine.ECONOMICS;
import static com.packsmart.engine.TestEngine.THICKNESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import com.packsmart.service.engine.model.Economics;
import com.packsmart.service.engine.model.Layer;
import com.packsmart.service.engine.model.MaterialData;
import com.packsmart.service.engine.model.ThicknessChoice;
import org.junit.jupiter.api.Test;

class ThicknessAndEconomicsTest {

    private static MaterialData material(double otr25) {
        return new MaterialData("Test", "plastic", otr25, 10, 100, 0.92, -50, 80, true, false, true, true, 3, "PE", 2,
                false, null, null);
    }

    @Test
    void otrAt50umIsHalfOf25um() {
        assertThat(THICKNESS.otrAt(material(70), 50)).isCloseTo(35.0, within(1e-9));
    }

    @Test
    void choosesThinnestPassingGaugeWithinLimits() {
        // OTR_25 = 70 → need ≤ 35 → 50 µm is the first allowed gauge that works
        ThicknessChoice c = THICKNESS.choose(material(70), 35.0, null);
        assertThat(c.passes()).isTrue();
        assertThat(c.thicknessUm()).isEqualTo(50);
    }

    @Test
    void neverBelowMinimumThickness() {
        ThicknessChoice c = THICKNESS.choose(material(70), null, null);
        assertThat(c.thicknessUm()).isEqualTo(20);
    }

    @Test
    void failsWithReasonWhenEvenMaxIsNotEnough() {
        ThicknessChoice c = THICKNESS.choose(material(7874), 0.05, null);
        assertThat(c.passes()).isFalse();
        assertThat(c.thicknessUm()).isEqualTo(100);
        assertThat(c.reason()).startsWith("even at 100 µm OTR is");
    }

    @Test
    void gramsIdentity() {
        assertThat(ECONOMICS.grams(0.06, 50, 0.92)).isCloseTo(2.76, within(1e-9));
    }

    @Test
    void costAndCarbonForLdpe() {
        MaterialData ldpe = CATALOG.material("LDPE").orElseThrow();
        Economics e = ECONOMICS.compute(List.of(new Layer(ldpe, 50)), 0.06);
        assertThat(e.gramsPerPack()).isCloseTo(2.76, within(1e-9));
        assertThat(e.costPerPackInr()).isCloseTo(2.76 * 120 / 1000, within(1e-9));
        assertThat(e.costPer1000Inr()).isCloseTo(2.76 * 120, within(1e-9));
        assertThat(e.co2eKgPer1000()).isCloseTo(2.76 * 1.9, within(1e-9));
    }
}
