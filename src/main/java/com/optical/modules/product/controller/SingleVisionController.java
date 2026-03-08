package com.optical.modules.product.controller;

import com.optical.modules.product.dto.SingleVisionCreateRequest;
import com.optical.modules.product.dto.SingleVisionCreateResponse;
import com.optical.modules.product.service.SingleVisionService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/lenses/single-vision")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Single Vision Lenses", description = "Single vision lens management APIs")
@SecurityRequirement(name = "bearerAuth")
public class SingleVisionController {

    private final SingleVisionService singleVisionService;

    @PostMapping
    @Operation(summary = "Create single vision lenses", description = "Creates one or many single vision lens variants from a single request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully", content = @Content(schema = @Schema(implementation = SingleVisionCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product type/UOM/Supplier not found")
    })
    public SingleVisionCreateResponse create(@Valid @RequestBody SingleVisionCreateRequest request) {
        return singleVisionService.create(request);
    }
}
