package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Sunglasses list item with selected details")
public class SunglassesListResponse {

    @Schema(description = "Sunglasses id")
    private Long id;

    @Schema(description = "Model name")
    private String modelName;

    @Schema(description = "Company")
    private String company;

    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal salesPrice;
    private List<SupplierInfoResponse> suppliers;
}
