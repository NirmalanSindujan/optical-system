package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Detailed sunglasses response")
public class SunglassesDetailResponse {

    private Long productId;
    private Long variantId;
    private String companyName;
    private String name;
    private String description;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String notes;
    private List<Long> supplierIds;
}
