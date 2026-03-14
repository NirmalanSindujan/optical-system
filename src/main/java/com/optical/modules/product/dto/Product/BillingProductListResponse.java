package com.optical.modules.product.dto.Product;

import com.optical.modules.product.dto.ProductVariantType;
import com.optical.modules.product.dto.SupplierInfoResponse;

import java.math.BigDecimal;
import java.util.List;

public class ProductListResonse {

    private Long productId;
    private Long variantId;
    private String productTypeCode;
    private String brandName;
    private String name;
    private String description;
    private Boolean productActive;
    private Boolean variantActive;
    private String sku;
    private String barcode;
    private String uomCode;
    private String notes;
    private ProductVariantType variantType;
    private Long supplierId;
    private List<Long> supplierIds;
    private List<SupplierInfoResponse> suppliers;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private BigDecimal quantity;

}
