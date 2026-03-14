package com.optical.modules.product.dto;

import com.optical.modules.product.dto.Lense.LensSubType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Response payload after creating a contact lens item")
public class ContactLensCreateResponse {

    private Long productId;
    private Long variantId;
    private String sku;
    private String productTypeCode;
    private String companyName;
    private String productName;
    private LensSubType lensSubType;
    private String color;
    private String baseCurve;
    private String uomCode;
    private List<Long> supplierIds;
    private List<SupplierInfoResponse> suppliers;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private BigDecimal quantity;
    private String extra;
}
