package com.packsmart.engine;

import java.util.List;

import com.packsmart.config.EngineProperties;
import com.packsmart.entity.Commodity;
import com.packsmart.seed.CsvDataSeeder;
import com.packsmart.service.CatalogMapper;
import com.packsmart.service.engine.BarrierCalculator;
import com.packsmart.service.engine.LaminateService;
import com.packsmart.service.engine.MapCalculator;
import com.packsmart.service.engine.PackEconomics;
import com.packsmart.service.engine.RecommendationEngine;
import com.packsmart.service.engine.RequirementService;
import com.packsmart.service.engine.ScoringService;
import com.packsmart.service.engine.ShelfLifeService;
import com.packsmart.service.engine.ThicknessCalculator;
import com.packsmart.service.engine.model.Catalog;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.EngineResult;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.Priority;
import com.packsmart.service.engine.model.StorageType;
import com.packsmart.service.engine.model.Transport;

/** Wires the pure engine by hand and loads the catalog from the same CSV files the app seeds (no database). */
public final class TestEngine {

    public static final EngineProperties PROPS = new EngineProperties();
    public static final BarrierCalculator BARRIER = new BarrierCalculator(PROPS);
    public static final LaminateService LAMINATES = new LaminateService(PROPS);
    public static final ThicknessCalculator THICKNESS = new ThicknessCalculator(PROPS, LAMINATES);
    public static final PackEconomics ECONOMICS = new PackEconomics();
    public static final ScoringService SCORING = new ScoringService(PROPS);
    public static final ShelfLifeService SHELF_LIFE = new ShelfLifeService(PROPS);
    public static final MapCalculator MAP = new MapCalculator(PROPS, BARRIER, THICKNESS);
    public static final RequirementService REQUIREMENTS = new RequirementService(PROPS);
    public static final RecommendationEngine ENGINE = new RecommendationEngine(PROPS, REQUIREMENTS, BARRIER, LAMINATES,
            THICKNESS, ECONOMICS, SCORING, SHELF_LIFE, MAP, null);

    public static final Catalog CATALOG = CatalogMapper.toCatalog(
            CsvDataSeeder.readClasspath("materials", CsvDataSeeder::material),
            CsvDataSeeder.readClasspath("material_extras", CsvDataSeeder::materialExtra),
            CsvDataSeeder.readClasspath("laminates", CsvDataSeeder::laminate),
            CsvDataSeeder.readClasspath("map_targets", CsvDataSeeder::mapTarget),
            LAMINATES);
    private static final List<Commodity> COMMODITIES = CsvDataSeeder.readClasspath("commodities", CsvDataSeeder::commodity);

    private TestEngine() {
    }

    public static FoodProfile food(String name) {
        return COMMODITIES.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst()
                .map(CatalogMapper::toFood)
                .orElseThrow(() -> new IllegalArgumentException("No commodity " + name));
    }

    public static Conditions conditions(double weightG, int days, StorageType storage, double tempC, double rh) {
        return new Conditions(weightG, BARRIER.estimateArea(weightG), true, days, storage, tempC, rh, Transport.LOCAL,
                Priority.DEFAULT);
    }

    public static EngineResult run(String food, double weightG, int days, StorageType storage, double tempC, double rh) {
        return ENGINE.compute(food(food), conditions(weightG, days, storage, tempC, rh), CATALOG);
    }
}
