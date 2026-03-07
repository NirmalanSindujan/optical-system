package com.optical.modules.product.controller;

import com.optical.modules.product.dto.AccessoryCreateRequest;
import com.optical.modules.product.dto.AccessoryDetailResponse;
import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.ProductPageResponse;
import com.optical.modules.product.service.AccessoryService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/accessories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Accessories", description = "Accessory management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AccessoryController {

    private final AccessoryService accessoryService;

    @GetMapping
    @Operation(summary = "List accessories", description = "Paginated accessories products.")
    public ProductPageResponse getAccessories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return accessoryService.search(q, page, size);
    }

    @PostMapping
    @Operation(summary = "Create accessory", description = "Creates an accessory product using the dedicated accessory workflow.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully", content = @Content(schema = @Schema(implementation = ProductCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product type/UOM/Supplier not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU")
    })
    public ProductCreateResponse createAccessory(@Valid @RequestBody AccessoryCreateRequest request) {
        return accessoryService.create(request);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get accessory by id", description = "Returns detailed accessory data by product id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found", content = @Content(schema = @Schema(implementation = AccessoryDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Accessory not found")
    })
    public AccessoryDetailResponse getAccessoryById(@PathVariable Long productId) {
        return accessoryService.getById(productId);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update accessory", description = "Updates accessory details by product id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully", content = @Content(schema = @Schema(implementation = AccessoryDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Accessory/Supplier not found")
    })
    public AccessoryDetailResponse updateAccessory(
            @PathVariable Long productId,
            @Valid @RequestBody AccessoryCreateRequest request
    ) {
        return accessoryService.update(productId, request);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete accessory", description = "Soft deletes an accessory product and its variants.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Accessory not found")
    })
    public void deleteAccessory(@PathVariable Long productId) {
        accessoryService.delete(productId);
    }
}
