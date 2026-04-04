package com.optical.modules.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InventoryItemResponse {

    private Long branchId;
    private String branchName;
    private Long variantId;
    private String productName;
    private String productTypeCode;
    private String lensSubType;
    private BigDecimal availableQuantity;
    private BigDecimal sellingPrice;
}
