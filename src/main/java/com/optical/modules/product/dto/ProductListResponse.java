package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Product list item combining common and type-specific fields")
public class ProductListResponse {

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

    private LensSubType lensSubType;
    private String material;
    private BigDecimal lensIndex;
    private String lensType;
    private String coatingCode;
    private BigDecimal sph;
    private BigDecimal cyl;
    private BigDecimal addPower;
    private String lensColor;
    private String baseCurve;

    private String frameCode;
    private String frameType;
    private String color;
    private String size;

    private String sunglassesDescription;
    private String itemType;
}
