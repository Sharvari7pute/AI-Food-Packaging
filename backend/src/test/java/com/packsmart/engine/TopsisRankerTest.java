package com.packsmart.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import com.packsmart.service.engine.TopsisRanker;
import com.packsmart.service.engine.TopsisRanker.Criterion;
import com.packsmart.service.engine.model.Candidate;
import com.packsmart.service.engine.model.EngineResult;
import com.packsmart.service.engine.model.StorageType;
import org.junit.jupiter.api.Test;

class TopsisRankerTest {

    private final TopsisRanker topsis = new TopsisRanker();

    @Test
    void dominatingAlternativeIsIdeal() {
        List<Double> c = topsis.closeness(
                List.of(new double[]{1, 10}, new double[]{3, 5}),
                List.of(new Criterion("quality", 0.5, true), new Criterion("cost", 0.5, false)));
        assertThat(c.get(1)).isCloseTo(1.0, within(1e-12));
        assertThat(c.get(0)).isCloseTo(0.0, within(1e-12));
    }

    @Test
    void weightsDecideTradeOffs() {
        // A: great barrier, expensive. B: weak barrier, cheap.
        List<double[]> m = List.of(new double[]{9, 10}, new double[]{2, 3});
        List<Double> barrierFirst = topsis.closeness(m, List.of(new Criterion("barrier", 0.8, true), new Criterion("cost", 0.2, false)));
        List<Double> budgetFirst = topsis.closeness(m, List.of(new Criterion("barrier", 0.2, true), new Criterion("cost", 0.8, false)));
        assertThat(barrierFirst.get(0)).isGreaterThan(barrierFirst.get(1));
        assertThat(budgetFirst.get(1)).isGreaterThan(budgetFirst.get(0));
    }

    @Test
    void knownThreeByThreeExample() {
        // Hand-checked: rows (7,9,9) (8,7,8) (9,6,8), all benefit, equal weights
        List<Double> c = topsis.closeness(
                List.of(new double[]{7, 9, 9}, new double[]{8, 7, 8}, new double[]{9, 6, 8}),
                List.of(new Criterion("a", 1.0 / 3, true), new Criterion("b", 1.0 / 3, true), new Criterion("c", 1.0 / 3, true)));
        assertThat(c.get(0)).isGreaterThan(c.get(1));
        assertThat(c).allSatisfy(x -> assertThat(x).isBetween(0.0, 1.0));
    }

    @Test
    void singleAlternativeGetsOne() {
        assertThat(topsis.closeness(List.<double[]>of(new double[]{1, 2}),
                List.of(new Criterion("a", 0.5, true), new Criterion("b", 0.5, false)))).containsExactly(1.0);
    }

    @Test
    void engineRanksPassingPacksByTopsis() {
        EngineResult r = TestEngine.run("Atta", 1000, 90, StorageType.AMBIENT, 30, 70);
        assertThat(r.options()).hasSizeGreaterThan(1);
        double prev = Double.MAX_VALUE;
        for (Candidate c : r.options()) {
            assertThat(c.getScores().topsis()).isNotNull().isBetween(0.0, 1.0);
            assertThat(c.getScores().topsis()).isLessThanOrEqualTo(prev);
            prev = c.getScores().topsis();
        }
        assertThat(r.options().get(0).getReasons().get(0)).contains("TOPSIS");
    }
}
