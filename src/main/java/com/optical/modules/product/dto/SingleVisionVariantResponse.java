package com.optical.modules.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SingleVisionVariantResponse {

    private Long productId;
    private Long variantId;
    private String sku;
    private BigDecimal sph;
    private BigDecimal cyl;
}
