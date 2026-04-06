package com.optical.modules.inventoryrequest.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InventoryRequestItemResponse {
    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String sku;
    private BigDecimal requestedQuantity;
    private String uomCode;
    private String uomName;
}
