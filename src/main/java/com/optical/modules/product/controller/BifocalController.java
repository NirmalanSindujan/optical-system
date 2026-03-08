package com.optical.modules.product.controller;

import com.optical.modules.product.dto.BifocalCreateRequest;
import com.optical.modules.product.dto.BifocalCreateResponse;
import com.optical.modules.product.dto.BifocalDetailResponse;
import com.optical.modules.product.dto.BifocalUpdateRequest;
import com.optical.modules.product.service.BifocalService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/lenses/bifocal")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Bifocal Lenses", description = "Bifocal lens management APIs")
@SecurityRequirement(name = "bearerAuth")
public class BifocalController {

    private final BifocalService bifocalService;

    @PostMapping
    @Operation(summary = "Create bifocal lenses", description = "Creates one or many bifocal lens variants from a single request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully", content = @Content(schema = @Schema(implementation = BifocalCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product type/UOM/Supplier not found")
    })
    public BifocalCreateResponse create(@Valid @RequestBody BifocalCreateRequest request) {
        return bifocalService.create(request);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get bifocal lens by id", description = "Returns detailed bifocal data by product id for UI editing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found", content = @Content(schema = @Schema(implementation = BifocalDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bifocal lens not found")
    })
    public BifocalDetailResponse getById(@PathVariable Long productId) {
        return bifocalService.getById(productId);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update bifocal lens", description = "Updates a single bifocal item without bulk fields. Quantity and purchase price are not editable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully", content = @Content(schema = @Schema(implementation = BifocalDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Bifocal lens or supplier not found")
    })
    public BifocalDetailResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody BifocalUpdateRequest request
    ) {
        return bifocalService.update(productId, request);
    }
}
