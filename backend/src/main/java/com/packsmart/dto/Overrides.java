package com.packsmart.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Per-request replacements for commodity properties (null = use database value). */
public record Overrides(
        @DecimalMin("0") @DecimalMax("100") Double moisturePct,
        @DecimalMin("0") @DecimalMax("1") Double waterActivity,
        @DecimalMin("0") @DecimalMax("100") Double fatPct,
        @DecimalMin("0") @DecimalMax("1000") Double respirationRate) {
}
