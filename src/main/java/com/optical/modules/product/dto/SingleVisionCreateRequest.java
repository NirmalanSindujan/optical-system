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
@Schema(description = "Create request for single vision lenses with single or range power generation")
public class SingleVisionCreateRequest {

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
    @Schema(description = "Add as a single power or generate a full range", requiredMode = Schema.RequiredMode.REQUIRED)
    private LensAdditionMethod additionMethod;

    @Schema(description = "When true, CYL values are required and combinations will be generated")
    private Boolean cylEnabled;

    @Schema(description = "Single mode only. Range -24 to 24, step 0.25")
    private BigDecimal sph;

    @Schema(description = "Single mode only when cylEnabled=true. Range -12 to 12, step 0.25")
    private BigDecimal cyl;

    @Schema(description = "Range mode only. Range -24 to 24, step 0.25")
    private BigDecimal sphStart;

    @Schema(description = "Range mode only. Range -24 to 24, step 0.25")
    private BigDecimal sphEnd;

    @Schema(description = "Range mode only when cylEnabled=true. Range -12 to 12, step 0.25")
    private BigDecimal cylStart;

    @Schema(description = "Range mode only when cylEnabled=true. Range -12 to 12, step 0.25")
    private BigDecimal cylEnd;

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
