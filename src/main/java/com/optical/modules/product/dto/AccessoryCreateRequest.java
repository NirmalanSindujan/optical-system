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
@Schema(description = "Simplified create request for accessories")
public class AccessoryCreateRequest {

    @NotBlank
    @Schema(description = "Company name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @NotBlank
    @Schema(description = "Model name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String modelName;

    @NotBlank
    @Schema(description = "Accessory type. Allowed values: Product, Service", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Required when type=Product")
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Required when type=Product")
    private BigDecimal purchasePrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Selling price", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal sellingPrice;

    @Schema(description = "Extra notes")
    private String extra;

    @Schema(description = "Optional when type=Service")
    private Long supplierId;

    @Schema(description = "Optional when type=Service")
    private List<Long> supplierIds;
}
