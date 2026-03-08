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
@Schema(description = "Create request for progressive lenses with single or range generation")
public class ProgressiveCreateRequest {

    @NotBlank
    @Schema(
            description = "Lens material. Allowed: Glass, Plastic Lense, Polycarbonate Lense",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String material;

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
    @Schema(description = "Quantity", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantity;

    @Schema(description = "When true, CYL values are required and combinations will be generated")
    private Boolean cylEnabled;

    @NotNull
    @Schema(description = "Add as a single SPH power or generate a SPH range", requiredMode = Schema.RequiredMode.REQUIRED)
    private LensAdditionMethod sphAdditionMethod;

    @Schema(description = "Add CYL as a single power or generate a CYL range when cylEnabled=true")
    private LensAdditionMethod cylAdditionMethod;

    @NotNull
    @Schema(description = "Add as a single ADD power or generate an ADD range", requiredMode = Schema.RequiredMode.REQUIRED)
    private LensAdditionMethod addAdditionMethod;

    @Schema(description = "Single SPH mode only. Range -24 to 24, step 0.25")
    private BigDecimal sph;

    @Schema(description = "Single CYL mode only when cylEnabled=true. Range -12 to 12, step 0.25")
    private BigDecimal cyl;

    @Schema(description = "SPH range mode only. Range -24 to 24, step 0.25")
    private BigDecimal sphStart;

    @Schema(description = "SPH range mode only. Range -24 to 24, step 0.25")
    private BigDecimal sphEnd;

    @Schema(description = "CYL range mode only when cylEnabled=true. Range -12 to 12, step 0.25")
    private BigDecimal cylStart;

    @Schema(description = "CYL range mode only when cylEnabled=true. Range -12 to 12, step 0.25")
    private BigDecimal cylEnd;

    @Schema(description = "Single ADD mode only. Range -4 to 4, step 0.25")
    private BigDecimal addPower;

    @Schema(description = "ADD range mode only. Range -4 to 4, step 0.25")
    private BigDecimal addPowerStart;

    @Schema(description = "ADD range mode only. Range -4 to 4, step 0.25")
    private BigDecimal addPowerEnd;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Purchase price", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal purchasePrice;

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
