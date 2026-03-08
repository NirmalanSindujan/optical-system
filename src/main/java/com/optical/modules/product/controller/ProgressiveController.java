package com.optical.modules.product.controller;

import com.optical.modules.product.dto.LensSubType;
import com.optical.modules.product.dto.ProductPageResponse;
import com.optical.modules.product.dto.ProgressiveCreateRequest;
import com.optical.modules.product.dto.ProgressiveCreateResponse;
import com.optical.modules.product.dto.ProgressiveDetailResponse;
import com.optical.modules.product.dto.ProgressiveUpdateRequest;
import com.optical.modules.product.service.ProductService;
import com.optical.modules.product.service.ProgressiveService;
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
@RequestMapping("/api/products/lenses/progressive")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Progressive Lenses", description = "Progressive lens management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProgressiveController {

    private final ProgressiveService progressiveService;
    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create progressive lenses", description = "Creates one or many progressive lens variants from a single request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully", content = @Content(schema = @Schema(implementation = ProgressiveCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product type/UOM/Supplier not found")
    })
    public ProgressiveCreateResponse create(@Valid @RequestBody ProgressiveCreateRequest request) {
        return progressiveService.create(request);
    }

    @GetMapping
    @Operation(summary = "List progressive lenses", description = "Paginated progressive lens list for UI tables.")
    public ProductPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return productService.searchLensSubtab(LensSubType.PROGRESSIVE.name(), q, page, size);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get progressive lens by id", description = "Returns detailed progressive data by product id for UI editing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found", content = @Content(schema = @Schema(implementation = ProgressiveDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Progressive lens not found")
    })
    public ProgressiveDetailResponse getById(@PathVariable Long productId) {
        return progressiveService.getById(productId);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update progressive lens", description = "Updates a single progressive item without bulk fields. Quantity and purchase price are not editable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully", content = @Content(schema = @Schema(implementation = ProgressiveDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Progressive lens or supplier not found")
    })
    public ProgressiveDetailResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ProgressiveUpdateRequest request
    ) {
        return progressiveService.update(productId, request);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete progressive lens", description = "Soft deletes a progressive lens product and its variants.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Progressive lens not found")
    })
    public void delete(@PathVariable Long productId) {
        productService.deleteProduct(productId);
    }
}
