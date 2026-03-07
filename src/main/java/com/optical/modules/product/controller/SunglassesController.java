package com.optical.modules.product.controller;

import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.SunglassesCreateRequest;
import com.optical.modules.product.dto.SunglassesDetailResponse;
import com.optical.modules.product.dto.SunglassesPageResponse;
import com.optical.modules.product.service.SunglassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/sunglasses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Sunglasses", description = "Sunglasses management APIs")
@SecurityRequirement(name = "bearerAuth")
public class SunglassesController {

    private final SunglassService sunglassService;

    @GetMapping
    @Operation(summary = "List sunglasses", description = "Paginated sunglasses products.")
    public SunglassesPageResponse getSunglasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return sunglassService.search(q, page, size);
    }

    @PostMapping
    @Operation(summary = "Create sunglasses", description = "Creates a sunglasses product using the dedicated sunglasses workflow.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully", content = @Content(schema = @Schema(implementation = ProductCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product type/UOM/Supplier not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU")
    })
    public ProductCreateResponse createSunglasses(@Valid @RequestBody SunglassesCreateRequest request) {
        return sunglassService.create(request);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get sunglasses by id", description = "Returns detailed sunglasses data by product id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found", content = @Content(schema = @Schema(implementation = SunglassesDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sunglasses not found")
    })
    public SunglassesDetailResponse getSunglassesById(@PathVariable Long productId) {
        return sunglassService.getById(productId);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update sunglasses", description = "Updates sunglasses details by product id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully", content = @Content(schema = @Schema(implementation = SunglassesDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Sunglasses/Supplier not found")
    })
    public SunglassesDetailResponse updateSunglasses(
            @PathVariable Long productId,
            @Valid @RequestBody SunglassesCreateRequest request
    ) {
        return sunglassService.update(productId, request);
    }
}
