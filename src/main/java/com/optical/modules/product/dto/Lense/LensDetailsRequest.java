package com.optical.modules.product.dto.Lense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Lens-specific details. UI should render 4 separate tabs by lensSubType.")
public class LensDetailsRequest {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LensSubType lensSubType;

    @Schema(description = "Required for SINGLE_VISION/BIFOCAL/PROGRESSIVE")
    private String material;
    @Schema(description = "Required for SINGLE_VISION/BIFOCAL/PROGRESSIVE")
    private BigDecimal lensIndex;
    @Schema(description = "Required for SINGLE_VISION only. Allowed: UC,HMC,PGHMC,PBHMC,BB,PGBB")
    private String lensType;
    private String coatingCode;
    @Schema(description = "Range -24 to 24, step 0.25 when provided")
    private BigDecimal sph;
    @Schema(description = "Range -12 to 12, step 0.25 when provided")
    private BigDecimal cyl;
    @Schema(description = "Range -4 to 4, step 0.25. Required for BIFOCAL/PROGRESSIVE")
    private BigDecimal addPower;
    @Schema(description = "Required for CONTACT_LENS")
    private String color;
    @Schema(description = "Required for CONTACT_LENS")
    private String baseCurve;
}
