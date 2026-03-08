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
@Schema(description = "Update request for a single contact lens item")
public class ContactLensUpdateRequest {

    @NotBlank
    @Schema(description = "Company name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @NotBlank
    @Schema(description = "Product name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank
    @Schema(description = "Lens color", requiredMode = Schema.RequiredMode.REQUIRED)
    private String color;

    @NotBlank
    @Schema(description = "Base curve", requiredMode = Schema.RequiredMode.REQUIRED)
    private String baseCurve;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Selling price", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal sellingPrice;

    @Schema(description = "Extra notes")
    private String extra;

    @Schema(description = "Single supplier id (legacy support)")
    private Long supplierId;

    @Schema(description = "Multiple supplier ids")
    private List<Long> supplierIds;
}
