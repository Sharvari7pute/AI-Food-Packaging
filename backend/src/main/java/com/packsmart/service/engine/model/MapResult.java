package com.packsmart.service.engine.model;

import java.util.List;

/** Step H output for respiring produce. {@code target} is null when map_targets has no row. */
public record MapResult(
        MapTargetData target,
        double respirationMlPerDay,
        double requiredOtr,
        double targetO2Pct,
        String filmMaterial,
        int filmThicknessUm,
        double filmOtr,
        boolean perforationNeeded,
        List<String> reasons,
        String note) {
}
