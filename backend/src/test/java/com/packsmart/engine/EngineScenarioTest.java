package com.packsmart.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.packsmart.service.engine.model.Candidate;
import com.packsmart.service.engine.model.EngineResult;
import com.packsmart.service.engine.model.Layer;
import com.packsmart.service.engine.model.MoistureMode;
import com.packsmart.service.engine.model.O2Barrier;
import com.packsmart.service.engine.model.StorageType;
import org.junit.jupiter.api.Test;

/** The end-to-end engine cases from MASTER_PROMPT Section 9, run on the real CSV data. */
class EngineScenarioTest {

    @Test
    void chips() {
        EngineResult r = TestEngine.run("Chips", 100, 90, StorageType.AMBIENT, 30, 70);
        assertThat(r.requirements().o2Barrier()).isEqualTo(O2Barrier.HIGH);
        assertThat(r.requirements().moistureMode()).isEqualTo(MoistureMode.KEEP_OUT);
        assertThat(r.requirements().opaque()).isTrue();
        assertThat(r.options()).isNotEmpty();
        assertThat(r.options().get(0).getLayers()).extracting(l -> l.material().name()).contains("Aluminium foil");
        assertThat(r.avoid()).isNotNull();
        assertThat(r.avoid().getName()).isEqualTo("LDPE");
        for (Candidate c : r.options()) {
            assertThat(c.getEval().otr()).isLessThanOrEqualTo(r.barrier().requiredOtr());
        }
        // fewer than 3 options → near misses are shown, never hidden
        if (r.options().size() < 3) {
            assertThat(r.nearMisses()).isNotEmpty();
        }
    }

    @Test
    void atta() {
        EngineResult r = TestEngine.run("Atta", 1000, 90, StorageType.AMBIENT, 30, 70);
        assertThat(r.requirements().moistureMode()).isEqualTo(MoistureMode.KEEP_OUT);
        assertThat(r.barrier().requiredOtr()).isNull();
        assertThat(r.barrier().requiredWvtr()).isNotNull();
        assertThat(r.options()).isNotEmpty();
        for (Candidate c : r.options()) {
            assertThat(c.getEval().wvtr()).isLessThanOrEqualTo(r.barrier().requiredWvtr());
        }
    }

    @Test
    void paneer() {
        EngineResult r = TestEngine.run("Paneer", 200, 7, StorageType.CHILLED, 4, 70);
        assertThat(r.requirements().moistureMode()).isEqualTo(MoistureMode.KEEP_IN);
        assertThat(r.requirements().o2Barrier()).isEqualTo(O2Barrier.MEDIUM);
        assertThat(r.options()).isNotEmpty();
        for (Candidate c : r.options()) {
            assertThat(c.getEval().heatSealable()).isTrue();
            assertThat(c.getEval().otr()).isLessThanOrEqualTo(r.barrier().requiredOtr());
        }
    }

    @Test
    void frozenMatar() {
        EngineResult r = TestEngine.run("Frozen matar", 500, 180, StorageType.FROZEN, -18, 70);
        assertThat(r.requirements().frozen()).isTrue();
        assertThat(r.options()).isNotEmpty();
        for (Candidate c : r.options()) {
            assertThat(c.getEval().minTempC()).isLessThanOrEqualTo(-25);
            for (Layer l : c.getLayers()) {
                assertThat(l.material().minTempC()).isLessThanOrEqualTo(-25);
            }
        }
    }

    @Test
    void tamatarUsesMap() {
        EngineResult r = TestEngine.run("Tamatar", 500, 7, StorageType.CHILLED, 12, 90);
        assertThat(r.requirements().needsMap()).isTrue();
        assertThat(r.requirements().moistureMode()).isEqualTo(MoistureMode.BREATHABLE);
        assertThat(r.map()).isNotNull();
        assertThat(r.map().target()).isNotNull();
        assertThat(r.map().target().o2Min()).isEqualTo(3);
        assertThat(r.map().target().o2Max()).isEqualTo(5);
        assertThat(r.map().perforationNeeded() || r.map().filmOtr() >= r.map().requiredOtr()).isTrue();
        assertThat(r.options()).isNotEmpty();
        Candidate top = r.options().get(0);
        assertThat(top.isPerforationNeeded() || top.getEval().otr() >= r.map().requiredOtr()).isTrue();
    }

    @Test
    void longDistanceNeedsStrength() {
        var food = TestEngine.food("Biscuit");
        var base = TestEngine.conditions(200, 120, StorageType.AMBIENT, 30, 70);
        var cond = new com.packsmart.service.engine.model.Conditions(base.packWeightG(), base.areaM2(), true,
                base.shelfLifeDays(), base.storageType(), base.storageTempC(), base.relativeHumidityPct(),
                com.packsmart.service.engine.model.Transport.LONG_DISTANCE, base.priority());
        EngineResult r = TestEngine.ENGINE.compute(food, cond, TestEngine.CATALOG);
        for (Candidate c : r.options()) {
            assertThat(c.getEval().strength()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void shelfLifeCurveIsBounded() {
        EngineResult r = TestEngine.run("Chips", 100, 90, StorageType.AMBIENT, 30, 70);
        for (Candidate c : r.options()) {
            assertThat(c.getShelfLife().curve()).hasSizeLessThanOrEqualTo(60);
            assertThat(c.getShelfLife().estimatedDays()).isLessThanOrEqualTo(730);
        }
    }
}
