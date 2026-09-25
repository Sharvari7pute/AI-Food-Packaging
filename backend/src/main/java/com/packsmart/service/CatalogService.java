package com.packsmart.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.packsmart.dto.CatalogDtos.CityDto;
import com.packsmart.dto.CatalogDtos.CommodityDto;
import com.packsmart.dto.CatalogDtos.CommoditySummary;
import com.packsmart.dto.CatalogDtos.LaminateDto;
import com.packsmart.dto.CatalogDtos.LayerView;
import com.packsmart.dto.CatalogDtos.MaterialDto;
import com.packsmart.entity.Commodity;
import com.packsmart.entity.Material;
import com.packsmart.entity.MaterialExtra;
import com.packsmart.exception.NotFoundException;
import com.packsmart.repository.CityRepository;
import com.packsmart.repository.CommodityRepository;
import com.packsmart.repository.LaminateRepository;
import com.packsmart.repository.MapTargetRepository;
import com.packsmart.repository.MaterialExtraRepository;
import com.packsmart.repository.MaterialRepository;
import com.packsmart.service.engine.BarrierCalculator;
import com.packsmart.service.engine.LaminateService;
import com.packsmart.service.engine.Num;
import com.packsmart.service.engine.PackEconomics;
import com.packsmart.service.engine.model.Catalog;
import com.packsmart.service.engine.model.Economics;
import com.packsmart.service.engine.model.LaminateDef;
import com.packsmart.service.engine.model.LaminateEval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Reads the (small, rarely changing) data tables and caches them in memory.
 * The cache is refreshed after seeding; data only changes through a reseed at startup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    /** Laminate library cost/CO2e is shown for a reference 100 g pack. */
    private static final double REFERENCE_PACK_G = 100;

    private final MaterialRepository materials;
    private final MaterialExtraRepository extras;
    private final LaminateRepository laminates;
    private final CommodityRepository commodities;
    private final MapTargetRepository mapTargets;
    private final CityRepository cities;
    private final LaminateService laminateService;
    private final PackEconomics economics;
    private final BarrierCalculator barrier;

    private volatile Snapshot snapshot;

    private record Snapshot(Catalog catalog, List<MaterialDto> materials, List<LaminateDto> laminates,
                            List<Commodity> commodities, List<CityDto> cities) {
    }

    /** Reloads everything from the database. */
    public synchronized void refresh() {
        List<Material> mats = materials.findAll();
        List<MaterialExtra> ext = extras.findAll();
        Catalog catalog = CatalogMapper.toCatalog(mats, ext, laminates.findAll(), mapTargets.findAll(), laminateService);

        Map<String, MaterialExtra> extraByName = new HashMap<>();
        ext.forEach(e -> extraByName.put(CatalogMapper.key(e.getName()), e));
        List<MaterialDto> matDtos = mats.stream()
                .sorted(Comparator.comparing(Material::getId))
                .map(m -> toDto(m, extraByName.get(CatalogMapper.key(m.getName()))))
                .toList();

        double refArea = barrier.estimateArea(REFERENCE_PACK_G);
        Map<String, Long> lamIds = new HashMap<>();
        laminates.findAll().forEach(l -> lamIds.put(l.getName(), l.getId()));
        List<LaminateDto> lamDtos = catalog.laminates().stream()
                .map(l -> toDto(lamIds.get(l.name()), l, refArea))
                .toList();

        List<CityDto> cityDtos = cities.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getName()))
                .map(c -> new CityDto(c.getId(), c.getName(), c.getState(), c.getSummerTempC(), c.getSummerRhPct(),
                        c.getMonsoonTempC(), c.getMonsoonRhPct(), c.getWinterTempC(), c.getWinterRhPct()))
                .toList();
        List<Commodity> comms = commodities.findAll().stream().sorted(Comparator.comparing(Commodity::getId)).toList();
        snapshot = new Snapshot(catalog, matDtos, lamDtos, comms, cityDtos);
    }

    private Snapshot snap() {
        Snapshot s = snapshot;
        if (s == null) {
            refresh();
            s = snapshot;
        }
        return s;
    }

    public Catalog catalog() {
        return snap().catalog();
    }

    public List<MaterialDto> materials() {
        return snap().materials();
    }

    public List<LaminateDto> laminates() {
        return snap().laminates();
    }

    public List<CityDto> cities() {
        return snap().cities();
    }

    public List<Commodity> commodityEntities() {
        return snap().commodities();
    }

    public List<CommoditySummary> commoditySummaries() {
        return snap().commodities().stream()
                .map(c -> new CommoditySummary(c.getId(), c.getName(), c.getNameHi(), c.getCategory(),
                        Boolean.TRUE.equals(c.getRespiring())))
                .toList();
    }

    public Commodity commodityEntity(Long id) {
        return snap().commodities().stream().filter(c -> c.getId().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Commodity " + id + " not found"));
    }

    public Optional<Commodity> commodityByName(String name) {
        return snap().commodities().stream().filter(c -> c.getName().equalsIgnoreCase(name.trim())).findFirst();
    }

    public CommodityDto commodity(Long id) {
        Commodity c = commodityEntity(id);
        return new CommodityDto(c.getId(), c.getName(), c.getNameHi(), c.getCategory(), c.getMoisturePct(),
                c.getWaterActivity(), c.getCriticalAw(), c.getFatPct(), c.getO2Sensitive(), c.getLightSensitive(),
                Boolean.TRUE.equals(c.getRespiring()), c.getRespirationRate(), c.getDefaultShelfLifeDays(),
                c.getSourceUrl(), c.getNotes(), CatalogMapper.isApprox(c.getNotes()));
    }

    /** Logs join-key problems between the CSV files so the team can fix them. */
    public void logDataWarnings() {
        Set<String> names = new HashSet<>();
        materials.findAll().forEach(m -> names.add(CatalogMapper.key(m.getName())));
        extras.findAll().stream().filter(e -> !names.contains(CatalogMapper.key(e.getName())))
                .forEach(e -> log.warn("material_extras.csv row '{}' matches no material name", e.getName()));
        materials.findAll().stream()
                .filter(m -> extras.findAll().stream().noneMatch(e -> CatalogMapper.key(e.getName()).equals(CatalogMapper.key(m.getName()))))
                .forEach(m -> log.warn("Material '{}' has no row in material_extras.csv (family/CO2e defaulted)", m.getName()));
        commodities.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getRespiring()))
                .filter(c -> mapTargets.findByNameIgnoreCase(c.getName()).isEmpty())
                .forEach(c -> log.warn("Respiring commodity '{}' has no map_targets.csv row", c.getName()));
    }

    private MaterialDto toDto(Material m, MaterialExtra x) {
        return new MaterialDto(m.getId(), m.getName(), m.getType(), m.getOtr25um(), m.getWvtr25um(), m.getCostPerKgInr(),
                m.getDensityGCm3(), m.getMinTempC(), m.getMaxTempC(), m.getRecyclable(), m.getBiodegradable(),
                m.getTransparent(), m.getHeatSealable(), m.getStrength1to5(), m.getSourceUrl(), m.getNotes(),
                x != null ? x.getFamily() : null, x != null ? x.getCo2eKgPerKg() : null,
                CatalogMapper.isApprox(m.getNotes()), "rigid".equalsIgnoreCase(m.getType()));
    }

    private LaminateDto toDto(Long id, LaminateDef l, double refArea) {
        LaminateEval e = laminateService.evaluate(l.layers());
        Economics econ = economics.compute(l.layers(), refArea);
        return new LaminateDto(id, l.name(), l.typicalUse(),
                l.layers().stream().map(x -> new LayerView(x.material().name(), x.thicknessUm())).toList(),
                e.totalThicknessUm(), Num.sig(e.otr()), Num.sig(e.wvtr()), e.minTempC(), e.maxTempC(), e.transparent(),
                e.heatSealable(), e.strength(), e.recyclable(), e.biodegradable(), e.family(), e.approx(),
                Num.sig(refArea), Num.dp(econ.costPer1000Inr(), 2), Num.dp(econ.co2eKgPer1000(), 3));
    }
}
