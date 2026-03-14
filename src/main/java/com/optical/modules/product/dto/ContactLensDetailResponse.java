package com.optical.modules.product.dto;

import com.optical.modules.product.dto.Lense.LensSubType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Detailed contact-lens response for single-item editing")
public class ContactLensDetailResponse {

    private Long productId;
    private Long variantId;
    private String companyName;
    private String name;
    private LensSubType lensSubType;
    private String color;
    private String baseCurve;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String extra;
    private List<Long> supplierIds;
    private List<SupplierInfoResponse> suppliers;
}
