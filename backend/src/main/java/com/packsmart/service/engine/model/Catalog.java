package com.packsmart.service.engine.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Everything the engine needs from the database, passed in so the engine stays pure. */
public record Catalog(List<MaterialData> materials, List<LaminateDef> laminates, Map<String, MapTargetData> mapTargets) {

    public Optional<MaterialData> material(String name) {
        return materials.stream().filter(m -> m.name().equalsIgnoreCase(name.trim())).findFirst();
    }

    public Optional<MapTargetData> mapTarget(String commodityName) {
        return Optional.ofNullable(mapTargets.get(commodityName.toLowerCase()));
    }
}
