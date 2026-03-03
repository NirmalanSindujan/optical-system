package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Frame-specific details")
public class FrameDetailsRequest {

    private String frameCode;
    @Schema(description = "Required for FRAME variant")
    private String frameType;
    private String color;
    private String size;
}
