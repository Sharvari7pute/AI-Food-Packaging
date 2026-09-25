package com.packsmart.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.packsmart.entity.Commodity;
import com.packsmart.seed.CsvDataSeeder;
import com.packsmart.service.CatalogMapper;
import com.packsmart.service.engine.model.Candidate;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.EngineResult;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.MoistureMode;
import com.packsmart.service.engine.model.O2Barrier;
import com.packsmart.service.engine.model.Priority;
import com.packsmart.service.engine.model.StorageType;
import com.packsmart.service.engine.model.Transport;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Validation report: runs the engine for every food with default inputs and checks whether the engine's derived
 * requirements agree with commodity_validation.csv (literature packaging direction). The validation file is only
 * read here - it is never seeded or fed into the engine. Report: target/validation-report.md.
 */
class CommodityValidationTest {

    /** Default pack for the report (same as the wizard default). */
    private static final double PACK_WEIGHT_G = 100;
    private static final double AMBIENT_RH = 65;
    private static final double COLD_RH = 90;
    private static final double FROZEN_MAX_C = -10;
    private static final double CHILLED_MAX_C = 15;
    /** Regression guard: the match count may not drop below the level agreed on when the report was added. */
    private static final int MIN_MATCHES = 22;

    record Expected(String name, String form, String direction, String deterioration) {
    }

    record Row(String name, String direction, String form, String inputs, String engineSays, String topPack, boolean match) {
    }

    @Test
    void engineAgreesWithLiteratureDirections() throws IOException {
        List<Expected> expected;
        try (Reader r = new InputStreamReader(new ClassPathResource("validation/commodity_validation.csv").getInputStream(),
                StandardCharsets.UTF_8)) {
            expected = CsvDataSeeder.parse(r, "commodity_validation",
                    rec -> new Expected(rec.get("name").trim(), rec.get("recommended_packaging_form"),
                            rec.get("recommended_packaging_direction"), rec.get("main_deterioration_factor")));
        }
        List<Commodity> foods = CsvDataSeeder.readClasspath("commodities", CsvDataSeeder::commodity);
        assertThat(expected).hasSize(foods.size());

        List<Row> rows = new ArrayList<>();
        for (Expected e : expected) {
            Commodity c = foods.stream().filter(f -> f.getName().equalsIgnoreCase(e.name())).findFirst()
                    .orElseThrow(() -> new AssertionError("Validation food not in commodities.csv: " + e.name()));
            FoodProfile food = CatalogMapper.toFood(c);
            Conditions cond = defaultConditions(c);
            EngineResult r = TestEngine.ENGINE.compute(food, cond, TestEngine.CATALOG);
            rows.add(new Row(e.name(), e.direction(), e.form(), describe(cond), describe(r), topPack(r), matches(e.direction(), r)));
        }
        String report = report(rows);
        Path out = Path.of("target", "validation-report.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report);
        System.out.println(report);

        long matched = rows.stream().filter(Row::match).count();
        assertThat(matched).as("foods whose engine requirements match the literature direction").isGreaterThanOrEqualTo(MIN_MATCHES);
    }

    /** Default inputs: 100 g pack, the food's default shelf life, the middle of its recommended storage range. */
    static Conditions defaultConditions(Commodity c) {
        double min = c.getStorageTempMinC() != null ? c.getStorageTempMinC() : 25;
        double max = c.getStorageTempMaxC() != null ? c.getStorageTempMaxC() : 25;
        double temp = (min + max) / 2.0;
        StorageType type = max <= FROZEN_MAX_C ? StorageType.FROZEN : max <= CHILLED_MAX_C ? StorageType.CHILLED : StorageType.AMBIENT;
        double rh = type == StorageType.AMBIENT ? AMBIENT_RH : COLD_RH;
        int days = Math.min(730, c.getDefaultShelfLifeDays() != null ? c.getDefaultShelfLifeDays() : 30);
        return new Conditions(PACK_WEIGHT_G, TestEngine.BARRIER.estimateArea(PACK_WEIGHT_G), true, days, type, temp, rh,
                Transport.LOCAL, Priority.DEFAULT);
    }

    /** Maps the literature direction to what the engine should derive. */
    static boolean matches(String direction, EngineResult r) {
        var q = r.requirements();
        boolean o2 = q.o2Barrier() != O2Barrier.LOW;
        return switch (direction) {
            case "high_oxygen_and_moisture_barrier" -> q.o2Barrier() == O2Barrier.HIGH && q.moistureMode() == MoistureMode.KEEP_OUT;
            case "moisture_barrier" -> q.moistureMode() == MoistureMode.KEEP_OUT;
            case "moisture_retention" -> q.moistureMode() == MoistureMode.KEEP_IN;
            case "light_barrier" -> q.opaque();
            case "light_and_oxygen_barrier" -> q.opaque() && o2;
            case "oxygen_barrier", "high_barrier_vacuum" -> o2;
            case "MAP", "breathable" -> q.needsMap();
            case "freezer_barrier" -> q.frozen() && !r.options().isEmpty()
                    && r.options().stream().allMatch(c -> c.getEval().minTempC() <= TestEngine.PROPS.getFrozenMinTempC());
            default -> false;
        };
    }

    private static String describe(Conditions c) {
        return String.format("%s %s °C, %s%% RH, %d d", c.storageType(), fmt(c.storageTempC()), fmt(c.relativeHumidityPct()),
                c.shelfLifeDays());
    }

    private static String describe(EngineResult r) {
        var q = r.requirements();
        List<String> parts = new ArrayList<>();
        parts.add("O₂ " + q.o2Barrier());
        parts.add(q.moistureMode().name());
        if (q.opaque()) {
            parts.add("opaque");
        }
        if (q.frozen()) {
            parts.add("frozen");
        }
        return String.join(", ", parts);
    }

    private static String topPack(EngineResult r) {
        if (r.options().isEmpty()) {
            return r.nearMisses().isEmpty() ? "none" : "none (best near miss: " + r.nearMisses().get(0).getName() + ")";
        }
        Candidate top = r.options().get(0);
        String layers = String.join(" / ", top.getLayers().stream()
                .map(l -> l.material().name() + " " + fmt(l.thicknessUm())).toList());
        return top.getName() + " (" + layers + " µm), " + top.getShelfLife().estimatedDays() + " d";
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String report(List<Row> rows) {
        long matched = rows.stream().filter(Row::match).count();
        StringBuilder sb = new StringBuilder();
        sb.append("# Engine validation against commodity_validation.csv\n\n");
        sb.append("Generated by `CommodityValidationTest`. The validation file is **not** used by the engine; ")
                .append("it only checks the engine's output.\n\n");
        sb.append("Default inputs per food: ").append(fmt(PACK_WEIGHT_G))
                .append(" g pack, the food's default shelf life (max 730 d), the middle of its recommended storage range ")
                .append("(FROZEN if max ≤ −10 °C, CHILLED if max ≤ 15 °C, else AMBIENT), RH 65 % ambient / 90 % cold.\n\n");
        sb.append("**Result: ").append(matched).append(" of ").append(rows.size())
                .append(" foods match the recommended packaging direction.**\n\n");
        sb.append("| Food | Literature direction | Literature form | Default inputs | Engine requirements | Engine top pack | Match |\n");
        sb.append("|---|---|---|---|---|---|---|\n");
        for (Row r : rows) {
            sb.append("| ").append(r.name()).append(" | ").append(r.direction()).append(" | ").append(r.form()).append(" | ")
                    .append(r.inputs()).append(" | ").append(r.engineSays()).append(" | ").append(r.topPack()).append(" | ")
                    .append(r.match() ? "✅" : "❌").append(" |\n");
        }
        sb.append("\n**How directions map to engine output:** high_oxygen_and_moisture_barrier = O₂ HIGH + KEEP_OUT; ")
                .append("moisture_barrier = KEEP_OUT; moisture_retention = KEEP_IN; light_barrier = opaque; ")
                .append("light_and_oxygen_barrier = opaque + O₂ not LOW; oxygen_barrier / high_barrier_vacuum = O₂ not LOW ")
                .append("(vacuum itself is not modelled); MAP / breathable = MAP; freezer_barrier = frozen-grade packs found.\n\n");
        sb.append("Packaging *form* (glass jar, tray, clamshell, nitrogen flush, vacuum) is not modelled by the engine and ")
                .append("is shown for reference only.\n");
        return sb.toString();
    }
}
