package com.optical.modules.product.controller;

import com.optical.common.base.PageResponse;
import com.optical.modules.product.dto.Lense.LensSubType;
import com.optical.modules.product.dto.Lense.LensDetailResponse;
import com.optical.modules.product.dto.Lense.LensSubtabResponse;
import com.optical.modules.product.dto.Product.BillingProductListResponse;
import com.optical.modules.product.dto.ProductCreateRequest;
import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.ProductPageResponse;
import com.optical.modules.product.dto.ProductVariantType;
import com.optical.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Products", description = "Generic product management APIs for lenses and shared product operations")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ProductCreateResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    @DeleteMapping("/{productId}")
    public void deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
    }

    @GetMapping("/lenses")
    @Operation(summary = "List lenses", description = "Paginated list for all lens items across all lens subtypes.")
    public ProductPageResponse getLenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return productService.searchLenses(q, page, size);
    }

    @GetMapping("/lenses/{variantId}")
    public LensDetailResponse getLensByVariantId(@PathVariable Long variantId) {
        return productService.getLensByVariantId(variantId);
    }

    @GetMapping("/lenses/subtabs")
    public List<LensSubtabResponse> getLensSubtabs() {
        return productService.getLensSubtabs();
    }

    @GetMapping("/lenses/subtabs/{lensSubType}")
    public ProductPageResponse getLensSubtabProducts(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Lens subtype tab",
                    schema = @Schema(implementation = LensSubType.class)
            )
            @PathVariable String lensSubType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return productService.searchLensSubtab(lensSubType, q, page, size);
    }

    @GetMapping("/productList")
    public PageResponse<BillingProductListResponse> getBillingProductList(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) ProductVariantType type,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
      return productService.getBillingProductList(q,page,size,supplierId,type);
    }




}
