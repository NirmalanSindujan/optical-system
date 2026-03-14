package com.optical.modules.product.dto;

import com.optical.modules.product.dto.Lense.LensSubType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Response payload after creating a product")
public class ProductCreateResponse {

    private Long productId;
    private Long variantId;
    private String productTypeCode;
    private String productName;
    private String sku;
    private String barcode;
    private ProductVariantType variantType;
    private LensSubType lensSubType;
    private Boolean productActive;
    private Boolean variantActive;
    private Long supplierId;
    private List<Long> supplierIds;
    private List<SupplierInfoResponse> suppliers;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private BigDecimal quantity;
}
