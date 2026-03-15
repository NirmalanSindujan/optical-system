package com.optical.modules.product.dto.Product;

import com.optical.modules.product.dto.Lense.LensSubType;
import com.optical.modules.product.dto.ProductVariantType;
import com.optical.modules.product.dto.SupplierInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class BillingProductListResponse {

    private Long productId;
    private Long variantId;
    private String productTypeCode;
    private String name;
    private String sku;
    private String barcode;
    private String uomCode;
    private ProductVariantType variantType;
    private BigDecimal sellingPrice;
    private BigDecimal quantity;
    private List<Long> supplierIds;
    private List<SupplierInfoResponse> suppliers;
    private LensSubType lensSubType;

    public BillingProductListResponse(
            Long productId,
            Long variantId,
            String productTypeCode,
            String name,
            String sku,
            String barcode,
            String uomCode,
            String variantType,
            BigDecimal sellingPrice,
            BigDecimal quantity,
            LensSubType lensSubType
    ) {
        this.productId = productId;
        this.variantId = variantId;
        this.productTypeCode = productTypeCode;
        this.name = name;
        this.sku = sku;
        this.barcode = barcode;
        this.uomCode = uomCode;
        this.variantType = variantType == null ? null : ProductVariantType.valueOf(variantType);
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.lensSubType = lensSubType;
    }

}
