package com.optical.modules.purchase.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class StockPurchaseItemResponse {

    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String sku;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal lineTotal;
    private String notes;
}
