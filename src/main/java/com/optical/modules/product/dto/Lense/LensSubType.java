package com.optical.modules.product.dto.Lense;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lens subtype used for 4 separate lens tabs in UI")
public enum LensSubType {
    SINGLE_VISION,
    BIFOCAL,
    PROGRESSIVE,
    CONTACT_LENS
}
