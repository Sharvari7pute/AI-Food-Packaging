package com.packsmart.seed;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.packsmart.config.AppProperties;
import com.packsmart.entity.City;
import com.packsmart.entity.Commodity;
import com.packsmart.entity.Laminate;
import com.packsmart.entity.MapTarget;
import com.packsmart.entity.Material;
import com.packsmart.entity.MaterialExtra;
import com.packsmart.repository.CityRepository;
import com.packsmart.repository.CommodityRepository;
import com.packsmart.repository.LaminateRepository;
import com.packsmart.repository.MapTargetRepository;
import com.packsmart.repository.MaterialExtraRepository;
import com.packsmart.repository.MaterialRepository;
import com.packsmart.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Loads the data tables from {@code classpath:data/*.csv} when they are empty.
 * With {@code RESEED_ON_START=true} the data tables (never {@code recommendations}) are wiped and reloaded.
 * A bad row is logged and skipped; it never crashes the app.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvDataSeeder implements ApplicationRunner {

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .setIgnoreSurroundingSpaces(true)
            .get();

    private final AppProperties app;
    private final MaterialRepository materials;
    private final MaterialExtraRepository extras;
    private final LaminateRepository laminates;
    private final CommodityRepository commodities;
    private final MapTargetRepository mapTargets;
    private final CityRepository cities;
    private final CatalogService catalog;
    private final TransactionTemplate tx;

    @Override
    public void run(ApplicationArguments args) {
        if (app.isReseedOnStart()) {
            log.warn("RESEED_ON_START=true -> deleting and reloading data tables (recommendations are kept)");
            tx.executeWithoutResult(s -> {
                materials.deleteAllInBatch();
                extras.deleteAllInBatch();
                laminates.deleteAllInBatch();
                commodities.deleteAllInBatch();
                mapTargets.deleteAllInBatch();
                cities.deleteAllInBatch();
            });
        }
        seed("materials", materials, CsvDataSeeder::material);
        seed("material_extras", extras, CsvDataSeeder::materialExtra);
        seed("laminates", laminates, CsvDataSeeder::laminate);
        seed("commodities", commodities, CsvDataSeeder::commodity);
        seed("map_targets", mapTargets, CsvDataSeeder::mapTarget);
        seed("cities", cities, CsvDataSeeder::city);
        catalog.refresh();
        catalog.logDataWarnings();
    }

    private <T> void seed(String table, JpaRepository<T, Long> repo, Function<CSVRecord, T> mapper) {
        long existing = repo.count();
        if (existing > 0) {
            log.info("Table {} already has {} rows - skipping seed", table, existing);
            return;
        }
        List<T> rows = readClasspath(table, mapper);
        tx.executeWithoutResult(s -> repo.saveAll(rows));
        log.info("Loaded {} {}", rows.size(), table);
    }

    /** Reads {@code classpath:data/<table>.csv}. */
    public static <T> List<T> readClasspath(String table, Function<CSVRecord, T> mapper) {
        ClassPathResource res = new ClassPathResource("data/" + table + ".csv");
        try (Reader reader = new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)) {
            return parse(reader, table, mapper);
        } catch (IOException e) {
            log.error("Could not read data/{}.csv: {}", table, e.getMessage());
            return List.of();
        }
    }

    /** Parses CSV rows with Apache Commons CSV; bad rows are logged and skipped. */
    public static <T> List<T> parse(Reader reader, String table, Function<CSVRecord, T> mapper) throws IOException {
        List<T> out = new ArrayList<>();
        try (CSVParser parser = FORMAT.parse(reader)) {
            for (CSVRecord rec : parser) {
                try {
                    T row = mapper.apply(rec);
                    if (row != null) {
                        out.add(row);
                    }
                } catch (RuntimeException e) {
                    log.warn("Skipping bad row {} in {}.csv: {}", rec.getRecordNumber(), table, e.getMessage());
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- row mappers

    public static Material material(CSVRecord r) {
        Material m = new Material();
        m.setName(required(r, "name"));
        m.setType(lower(str(r, "type")));
        m.setOtr25um(dbl(r, "otr_25um"));
        m.setWvtr25um(dbl(r, "wvtr_25um"));
        m.setCostPerKgInr(dbl(r, "cost_per_kg_inr"));
        m.setDensityGCm3(dbl(r, "density_g_cm3"));
        m.setMinTempC(dbl(r, "min_temp_c"));
        m.setMaxTempC(dbl(r, "max_temp_c"));
        m.setRecyclable(bool(r, "recyclable"));
        m.setBiodegradable(bool(r, "biodegradable"));
        m.setTransparent(bool(r, "transparent"));
        m.setHeatSealable(bool(r, "heat_sealable"));
        m.setStrength1to5(integer(r, "strength_1to5"));
        m.setSourceUrl(str(r, "source_url"));
        m.setNotes(str(r, "notes"));
        return m;
    }

    public static MaterialExtra materialExtra(CSVRecord r) {
        MaterialExtra e = new MaterialExtra();
        e.setName(required(r, "name"));
        e.setFamily(str(r, "family"));
        e.setCo2eKgPerKg(dbl(r, "co2e_kg_per_kg"));
        e.setNotes(str(r, "notes"));
        return e;
    }

    public static Laminate laminate(CSVRecord r) {
        Laminate l = new Laminate();
        l.setName(required(r, "name"));
        l.setLayers(required(r, "layers"));
        l.setTypicalUse(str(r, "typical_use"));
        return l;
    }

    public static Commodity commodity(CSVRecord r) {
        Commodity c = new Commodity();
        c.setName(required(r, "name"));
        c.setNameHi(str(r, "name_hi"));
        c.setCategory(str(r, "category"));
        c.setMoisturePct(dbl(r, "moisture_pct"));
        c.setWaterActivity(dbl(r, "water_activity"));
        c.setCriticalAw(dbl(r, "critical_aw"));
        c.setFatPct(dbl(r, "fat_pct"));
        c.setO2Sensitive(lower(str(r, "o2_sensitive")));
        c.setLightSensitive(lower(str(r, "light_sensitive")));
        c.setRespiring(bool(r, "respiring"));
        c.setRespirationRate(dbl(r, "respiration_rate"));
        c.setDefaultShelfLifeDays(integer(r, "default_shelf_life_days"));
        c.setPh(dbl(r, "ph"));
        c.setStorageTempMinC(dbl(r, "storage_temp_min_c"));
        c.setStorageTempMaxC(dbl(r, "storage_temp_max_c"));
        c.setMainDeteriorationFactor(lower(str(r, "main_deterioration_factor")));
        c.setSourceUrl(str(r, "source_url"));
        c.setNotes(str(r, "notes"));
        return c;
    }

    public static MapTarget mapTarget(CSVRecord r) {
        MapTarget t = new MapTarget();
        t.setName(required(r, "name"));
        t.setTargetO2Min(dbl(r, "target_o2_min"));
        t.setTargetO2Max(dbl(r, "target_o2_max"));
        t.setTargetCo2Min(dbl(r, "target_co2_min"));
        t.setTargetCo2Max(dbl(r, "target_co2_max"));
        t.setStorageTempC(dbl(r, "storage_temp_c"));
        t.setSourceUrl(str(r, "source_url"));
        return t;
    }

    public static City city(CSVRecord r) {
        City c = new City();
        c.setName(required(r, "name"));
        c.setState(str(r, "state"));
        c.setSummerTempC(dbl(r, "summer_temp_c"));
        c.setSummerRhPct(dbl(r, "summer_rh_pct"));
        c.setMonsoonTempC(dbl(r, "monsoon_temp_c"));
        c.setMonsoonRhPct(dbl(r, "monsoon_rh_pct"));
        c.setWinterTempC(dbl(r, "winter_temp_c"));
        c.setWinterRhPct(dbl(r, "winter_rh_pct"));
        return c;
    }

    // ---------------------------------------------------------------- cell helpers

    static String str(CSVRecord r, String col) {
        if (!r.isMapped(col) || !r.isSet(col)) {
            return null;
        }
        String v = r.get(col);
        return v == null || v.isBlank() ? null : v.trim();
    }

    static String required(CSVRecord r, String col) {
        String v = str(r, col);
        if (v == null) {
            throw new IllegalArgumentException("missing required column '" + col + "'");
        }
        return v;
    }

    static String lower(String v) {
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }

    static Double dbl(CSVRecord r, String col) {
        String v = str(r, col);
        if (v == null) {
            return null;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("column '" + col + "' is not a number: " + v);
        }
    }

    static Integer integer(CSVRecord r, String col) {
        Double d = dbl(r, col);
        return d == null ? null : (int) Math.round(d);
    }

    static Boolean bool(CSVRecord r, String col) {
        String v = lower(str(r, col));
        if (v == null) {
            return null;
        }
        return switch (v) {
            case "yes", "y", "true", "1" -> true;
            case "no", "n", "false", "0" -> false;
            default -> throw new IllegalArgumentException("column '" + col + "' must be yes/no: " + v);
        };
    }
}
