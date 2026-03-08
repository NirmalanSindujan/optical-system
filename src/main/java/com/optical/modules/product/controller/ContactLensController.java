package com.optical.modules.product.controller;

import com.optical.modules.product.dto.ContactLensCreateRequest;
import com.optical.modules.product.dto.ContactLensCreateResponse;
import com.optical.modules.product.dto.ContactLensDetailResponse;
import com.optical.modules.product.dto.ContactLensPageResponse;
import com.optical.modules.product.dto.ContactLensUpdateRequest;
import com.optical.modules.product.service.ContactLensService;
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
@RequestMapping("/api/products/lenses/contact-lens")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Contact Lenses", description = "Contact lens management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ContactLensController {

    private final ContactLensService contactLensService;

    @PostMapping
    @Operation(summary = "Create contact lens", description = "Creates a single contact lens item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully", content = @Content(schema = @Schema(implementation = ContactLensCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product type/UOM/Supplier not found")
    })
    public ContactLensCreateResponse create(@Valid @RequestBody ContactLensCreateRequest request) {
        return contactLensService.create(request);
    }

    @GetMapping
    @Operation(summary = "List contact lenses", description = "Paginated contact lens list for UI tables.")
    public ContactLensPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return contactLensService.search(q, page, size);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get contact lens by id", description = "Returns detailed contact-lens data by product id for UI editing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found", content = @Content(schema = @Schema(implementation = ContactLensDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contact lens not found")
    })
    public ContactLensDetailResponse getById(@PathVariable Long productId) {
        return contactLensService.getById(productId);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update contact lens", description = "Updates a single contact-lens item. Quantity and purchase price are not editable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully", content = @Content(schema = @Schema(implementation = ContactLensDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Contact lens or supplier not found")
    })
    public ContactLensDetailResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ContactLensUpdateRequest request
    ) {
        return contactLensService.update(productId, request);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete contact lens", description = "Soft deletes a contact lens product and its variants.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Contact lens not found")
    })
    public void delete(@PathVariable Long productId) {
        contactLensService.delete(productId);
    }
}
