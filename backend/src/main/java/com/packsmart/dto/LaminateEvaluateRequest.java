package com.packsmart.dto;

import java.util.List;

import com.packsmart.service.engine.model.StorageType;
import com.packsmart.service.engine.model.Transport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Laminate Builder input: custom layers (outside → inside), optional pack size and optional food test. */
public record LaminateEvaluateRequest(
        @NotEmpty @Size(max = 8) List<@Valid LayerInput> layers,
        @DecimalMin("1") @DecimalMax("50000") Double packWeightG,
        @DecimalMin("0.001") @DecimalMax("10") Double packAreaM2,
        @Valid FoodTest test) {

    public record LayerInput(@NotBlank String material, @NotNull @DecimalMin("1") @DecimalMax("500") Double thicknessUm) {
    }

    /** "Test against a food": checks the structure against that food's requirements. */
    public record FoodTest(
            @NotNull Long commodityId,
            @DecimalMin("1") @DecimalMax("50000") Double packWeightG,
            @NotNull @Min(1) @Max(730) Integer shelfLifeDays,
            @NotNull StorageType storageType,
            @NotNull @DecimalMin("-40") @DecimalMax("60") Double storageTempC,
            @NotNull @DecimalMin("0") @DecimalMax("100") Double relativeHumidityPct,
            Transport transport) {
    }
}
