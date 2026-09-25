package com.packsmart.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.packsmart.service.engine.model.Priority;
import com.packsmart.service.engine.model.StorageType;
import com.packsmart.service.engine.model.Transport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Engine input. Either {@code commodityId} (a food from the database) or {@code customCommodity}
 * (e.g. AI-estimated and user-edited) must be given.
 */
public record RecommendRequest(
        Long commodityId,
        @Valid CustomCommodity customCommodity,
        @Valid Overrides overrides,
        @NotNull @DecimalMin("1") @DecimalMax("50000") Double packWeightG,
        @DecimalMin(value = "0.001", message = "must be at least 0.001 m²") @DecimalMax("10") Double packAreaM2,
        @NotNull @Min(1) @Max(730) Integer shelfLifeDays,
        @NotNull StorageType storageType,
        @NotNull @DecimalMin("-40") @DecimalMax("60") Double storageTempC,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double relativeHumidityPct,
        Transport transport,
        Priority priority,
        @Pattern(regexp = "en|hi|mr", message = "must be en, hi or mr") String language) {

    @JsonIgnore
    @AssertTrue(message = "commodityId or customCommodity is required")
    public boolean isFoodSpecified() {
        return commodityId != null || customCommodity != null;
    }

    public Transport transportOrDefault() {
        return transport != null ? transport : Transport.LOCAL;
    }

    public Priority priorityOrDefault() {
        return priority != null ? priority : Priority.DEFAULT;
    }

    public String languageOrDefault() {
        return language != null ? language : "en";
    }
}
