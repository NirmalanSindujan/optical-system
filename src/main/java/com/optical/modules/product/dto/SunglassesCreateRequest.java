package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Schema(description = "Simplified create request for sunglasses")
public class SunglassesCreateRequest {

    @NotBlank
    @Schema(description = "Company/brand name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @NotBlank
    @Schema(description = "Product name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Description")
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Quantity", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Purchase price", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal purchasePrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Selling price", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal sellingPrice;

    @Schema(description = "Variant notes")
    private String notes;

    @Schema(description = "Single supplier id (legacy support)")
    private Long supplierId;

    @Schema(description = "Multiple supplier ids")
    private List<Long> supplierIds;
}
