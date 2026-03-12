package com.optical.modules.supplier.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SupplierProductStockResponse {

    private Long productId;
    private Long variantId;
    private String name;
    private String sku;
    private BigDecimal sellingPrice;
    private BigDecimal currentQuantity;
}
