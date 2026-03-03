package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Sunglasses-specific details")
public class SunglassesDetailsRequest {

    @Schema(description = "Required for SUNGLASSES variant")
    private String description;
}
