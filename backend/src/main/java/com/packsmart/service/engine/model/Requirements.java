package com.packsmart.service.engine.model;

import java.util.List;

/** Step A output: what the pack must do, with human-readable reasons. */
public record Requirements(
        O2Barrier o2Barrier,
        MoistureMode moistureMode,
        boolean opaque,
        boolean frozen,
        boolean needsMap,
        boolean needsStrength,
        List<String> reasons) {
}
