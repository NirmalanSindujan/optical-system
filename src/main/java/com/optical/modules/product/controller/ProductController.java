package com.optical.modules.product.controller;

import com.optical.modules.product.dto.LensSubType;
import com.optical.modules.product.dto.LensSubtabResponse;
import com.optical.modules.product.dto.ProductCreateRequest;
import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.ProductPageResponse;
import com.optical.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Create generic product",
            description = "Creates generic catalog products. Use dedicated frame and sunglasses controllers for those workflows."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully", content = @Content(schema = @Schema(implementation = ProductCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU/Barcode")
    })
    public ProductCreateResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete product", description = "Soft deletes a product and its variants for any product type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
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

    @GetMapping("/lenses/subtabs")
    @Operation(
            summary = "Get lens subtabs",
            description = "Returns 4 lens subtype tabs with counts: SINGLE_VISION, BIFOCAL, PROGRESSIVE, CONTACT_LENS."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lens subtabs",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = LensSubtabResponse.class)))
    )
    public List<LensSubtabResponse> getLensSubtabs() {
        return productService.getLensSubtabs();
    }

    @GetMapping("/lenses/subtabs/{lensSubType}")
    @Operation(summary = "List lenses by subtab", description = "Paginated list for one lens subtab.")
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

}
