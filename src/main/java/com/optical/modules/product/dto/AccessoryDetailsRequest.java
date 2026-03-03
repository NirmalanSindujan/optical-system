package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Accessory-specific details")
public class AccessoryDetailsRequest {

    @Schema(description = "Required for ACCESSORY variant")
    private String itemType;
}
