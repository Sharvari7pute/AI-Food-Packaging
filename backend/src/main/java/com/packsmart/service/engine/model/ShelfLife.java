package com.packsmart.service.engine.model;

import java.util.List;

/** Step G output. {@code o2Days}/{@code moistureDays} are null when that mechanism does not apply. */
public record ShelfLife(Double o2Days, Double moistureDays, int estimatedDays, LimitingFactor limitingFactor, List<CurvePoint> curve) {
}
