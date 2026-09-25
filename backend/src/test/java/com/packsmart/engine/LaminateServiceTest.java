package com.packsmart.engine;

import static com.packsmart.engine.TestEngine.CATALOG;
import static com.packsmart.engine.TestEngine.LAMINATES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import com.packsmart.service.engine.model.LaminateEval;
import com.packsmart.service.engine.model.Layer;
import org.junit.jupiter.api.Test;

class LaminateServiceTest {

    @Test
    void layersInSeriesCombineLikeResistors() {
        assertThat(LAMINATES.seriesTotal(List.of(100.0, 100.0))).isCloseTo(50.0, within(1e-9));
    }

    @Test
    void zeroTransmissionLayerIsPerfectBarrier() {
        assertThat(LAMINATES.seriesTotal(List.of(100.0, 0.0))).isZero();
    }

    @Test
    void thicknessScalesInversely() {
        assertThat(LAMINATES.layerTransmission(70, 50)).isCloseTo(35.0, within(1e-9));
    }

    @Test
    void parsesOnLastColonAndEvaluatesPetAlPe() {
        List<Layer> layers = LAMINATES.parseLayers("PET:12;Aluminium foil:9;LDPE:50", n -> CATALOG.material(n));
        assertThat(layers).extracting(l -> l.material().name()).containsExactly("PET", "Aluminium foil", "LDPE");
        LaminateEval e = LAMINATES.evaluate(layers);
        assertThat(e.totalThicknessUm()).isEqualTo(71);
        assertThat(e.heatSealable()).isTrue();
        assertThat(e.transparent()).isFalse();
        assertThat(e.recyclable()).isFalse();
        assertThat(e.family()).isEqualTo(LaminateEval.MIXED);
        assertThat(e.containsAluminium()).isTrue();
        assertThat(e.minTempC()).isEqualTo(-50);
        assertThat(e.maxTempC()).isEqualTo(80);
        assertThat(e.strength()).isEqualTo(5);
        assertThat(e.otr()).isLessThan(0.02);
    }

    @Test
    void monoPpLaminateIsRecyclable() {
        LaminateEval e = LAMINATES.evaluate(LAMINATES.parseLayers("BOPP:20;CPP:30", n -> CATALOG.material(n)));
        assertThat(e.recyclable()).isTrue();
        assertThat(e.family()).isEqualTo("PP");
    }

    @Test
    void materialNamesWithBracketsParse() {
        List<Layer> layers = LAMINATES.parseLayers("Nylon (PA):25;LDPE:60", n -> CATALOG.material(n));
        assertThat(layers.get(0).material().name()).isEqualTo("Nylon (PA)");
        assertThat(layers.get(0).thicknessUm()).isEqualTo(25);
    }

    @Test
    void unknownMaterialIsRejected() {
        assertThatThrownBy(() -> LAMINATES.parseLayers("Unobtainium:10", n -> CATALOG.material(n)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
