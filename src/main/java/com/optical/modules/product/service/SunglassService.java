package com.optical.modules.product.service;

import com.optical.modules.product.dto.ProductVariantType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SunglassService
{

    private static final String DEFAULT_PRODUCT_TYPE_CODE = ProductVariantType.SUNGLASSES.toString();

    private static final String DEFAULT_UOM_CODE = "EA";
    private static final String SUNGLASSES_SKU_PREFIX = "SUN-";



}
