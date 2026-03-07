package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Detailed frame response")
public class FrameDetailResponse {

    private Long productId;
    private Long variantId;
    private String name;
    private String code;
    private String type;
    private String color;
    private String size;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String extra;
    private List<Long> supplierIds;
    private List<SupplierInfoResponse> suppliers;
}
