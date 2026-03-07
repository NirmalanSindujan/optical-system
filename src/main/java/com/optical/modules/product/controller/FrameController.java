package com.optical.modules.product.controller;

import com.optical.modules.product.dto.FrameCreateRequest;
import com.optical.modules.product.dto.FrameDetailResponse;
import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.ProductPageResponse;
import com.optical.modules.product.service.FrameService;
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
@RequestMapping("/api/products/frames")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Frames", description = "Frame management APIs")
@SecurityRequirement(name = "bearerAuth")
public class FrameController {

    private final FrameService frameService;

    @GetMapping
    @Operation(summary = "List frames", description = "Paginated frame products.")
    public ProductPageResponse getFrames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return frameService.search(q, page, size);
    }

    @PostMapping
    @Operation(summary = "Create frame", description = "Creates a frame product using the dedicated frame workflow.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully", content = @Content(schema = @Schema(implementation = ProductCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product type/UOM/Supplier not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU")
    })
    public ProductCreateResponse createFrame(@Valid @RequestBody FrameCreateRequest request) {
        return frameService.create(request);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get frame by id", description = "Returns detailed frame data by product id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found", content = @Content(schema = @Schema(implementation = FrameDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Frame not found")
    })
    public FrameDetailResponse getFrameById(@PathVariable Long productId) {
        return frameService.getById(productId);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update frame", description = "Updates frame details by product id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully", content = @Content(schema = @Schema(implementation = FrameDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Frame/Supplier not found")
    })
    public FrameDetailResponse updateFrame(
            @PathVariable Long productId,
            @Valid @RequestBody FrameCreateRequest request
    ) {
        return frameService.update(productId, request);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete frame", description = "Soft deletes a frame product and its variants.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Frame not found")
    })
    public void deleteFrame(@PathVariable Long productId) {
        frameService.delete(productId);
    }
}
