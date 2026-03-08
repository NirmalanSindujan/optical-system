package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Detailed lens response")
public class LensDetailResponse {

    private Long productId;
    private Long variantId;
    private String companyName;
    private String name;
    private LensSubType lensSubType;
    private String material;
    private BigDecimal index;
    private String type;
    private String coatingCode;
    private BigDecimal sph;
    private BigDecimal cyl;
    private BigDecimal addPower;
    private String color;
    private String baseCurve;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String extra;
    private List<Long> supplierIds;
    private List<SupplierInfoResponse> suppliers;
}
