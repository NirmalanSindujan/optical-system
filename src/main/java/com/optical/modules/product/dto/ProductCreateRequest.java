package com.optical.modules.product.dto;

import com.optical.modules.product.dto.Lense.LensDetailsRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Create request for products. Exactly one details object must be provided based on variantType.")
public class ProductCreateRequest {

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String productTypeCode;

    private String brandName;

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    private String description;
    private Boolean isActive;

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    private String barcode;

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String uomCode;

    private String notes;
    private Boolean variantActive;
    @Schema(description = "Supplier reference id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long supplierId;

    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Purchase price", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal purchasePrice;

    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Selling price", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Required for non-lens products")
    private BigDecimal quantity;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ProductVariantType variantType;

    @Valid
    @Schema(description = "Required when variantType=LENS")
    private LensDetailsRequest lensDetails;

    @Valid
    @Schema(description = "Required when variantType=FRAME")
    private FrameDetailsRequest frameDetails;

    @Valid
    @Schema(description = "Required when variantType=SUNGLASSES")
    private SunglassesDetailsRequest sunglassesDetails;

    @Valid
    @Schema(description = "Required when variantType=ACCESSORY")
    private AccessoryDetailsRequest accessoryDetails;
}
