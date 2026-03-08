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
@Schema(description = "Update request for a single single-vision lens item")
public class SingleVisionUpdateRequest {

    @NotBlank
    @Schema(
            description = "Lens material. Allowed: Glass, Plastic Lense, Polycarbonate Lense",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String material;

    @NotBlank
    @Schema(description = "Lens type. Allowed: UC,HMC,PGHMC,PBHMC,BB,PGBB", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotBlank
    @Schema(description = "Company name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @NotBlank
    @Schema(description = "Product name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = "Lens index", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal index;

    @NotNull
    @Schema(description = "SPH value. Range -24 to 24, step 0.25", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal sph;

    @Schema(description = "CYL value. Range -12 to 12, step 0.25")
    private BigDecimal cyl;

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
