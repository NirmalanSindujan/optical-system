package com.optical.modules.inventoryrequest.controller;

import com.optical.modules.inventoryrequest.dto.InventoryRequestCreateRequest;
import com.optical.modules.inventoryrequest.dto.InventoryRequestDecisionRequest;
import com.optical.modules.inventoryrequest.dto.InventoryRequestPageResponse;
import com.optical.modules.inventoryrequest.dto.InventoryRequestResponse;
import com.optical.modules.inventoryrequest.entity.InventoryRequestStatus;
import com.optical.modules.inventoryrequest.service.InventoryRequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_USER')")
@Tag(name = "Inventory Requests", description = "Inter-branch inventory request APIs")
@SecurityRequirement(name = "bearerAuth")
public class InventoryRequestController {

    private final InventoryRequestService inventoryRequestService;

    @PostMapping
    public InventoryRequestResponse create(@Valid @RequestBody InventoryRequestCreateRequest request) {
        return inventoryRequestService.create(request);
    }

    @GetMapping("/{id}")
    public InventoryRequestResponse getById(@PathVariable Long id) {
        return inventoryRequestService.getById(id);
    }

    @GetMapping
    public InventoryRequestPageResponse search(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) InventoryRequestStatus status,
            @RequestParam(required = false) String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return inventoryRequestService.search(branchId, status, direction, page, size);
    }

    @PostMapping("/{id}/accept")
    public InventoryRequestResponse accept(
            @PathVariable Long id,
            @RequestBody(required = false) InventoryRequestDecisionRequest request
    ) {
        return inventoryRequestService.accept(id, request);
    }

    @PostMapping("/{id}/reject")
    public InventoryRequestResponse reject(
            @PathVariable Long id,
            @RequestBody(required = false) InventoryRequestDecisionRequest request
    ) {
        return inventoryRequestService.reject(id, request);
    }
}
