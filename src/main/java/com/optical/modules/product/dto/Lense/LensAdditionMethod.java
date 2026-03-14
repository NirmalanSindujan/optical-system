package com.optical.modules.product.dto.Lense;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "How single vision powers should be created")
public enum LensAdditionMethod {
    SINGLE,
    RANGE
}
