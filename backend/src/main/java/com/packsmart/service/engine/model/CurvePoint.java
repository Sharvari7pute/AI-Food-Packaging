package com.packsmart.service.engine.model;

/** A point on the shelf-life curve. A null percentage means that mechanism does not apply. */
public record CurvePoint(int day, Double o2UsedPct, Double moistureUsedPct) {
}
