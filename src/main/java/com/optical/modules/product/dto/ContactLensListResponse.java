package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Contact lens list item")
public class ContactLensListResponse {

    private Long productId;
    private Long variantId;
    private String companyName;
    private String name;
    private String sku;
    private String uomCode;
    private String color;
    private String baseCurve;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String extra;
    private List<Long> supplierIds;
    private List<SupplierInfoResponse> suppliers;
}
