package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Top-level product variant type")
public enum ProductVariantType {
    LENS,
    FRAME,
    SUNGLASSES,
    ACCESSORY
}
