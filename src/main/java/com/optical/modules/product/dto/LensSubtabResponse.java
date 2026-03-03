package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Lens subtab metadata for rendering 4 UI tabs")
public class LensSubtabResponse {

    private LensSubType lensSubType;
    private long totalCounts;
}
