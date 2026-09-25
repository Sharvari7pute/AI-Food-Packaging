package com.packsmart.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** A food that is not in the database (same fields as commodities.csv). Never saved as a commodity. */
public record CustomCommodity(
        @NotBlank String name,
        String nameHi,
        String category,
        @DecimalMin("0") @DecimalMax("100") Double moisturePct,
        @NotNull @DecimalMin("0") @DecimalMax("1") Double waterActivity,
        @DecimalMin("0") @DecimalMax("1") Double criticalAw,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double fatPct,
        @Pattern(regexp = "high|medium|low") String o2Sensitive,
        @Pattern(regexp = "high|medium|low") String lightSensitive,
        Boolean respiring,
        @DecimalMin("0") @DecimalMax("1000") Double respirationRate,
        Integer defaultShelfLifeDays,
        Boolean aiEstimated) {
}
