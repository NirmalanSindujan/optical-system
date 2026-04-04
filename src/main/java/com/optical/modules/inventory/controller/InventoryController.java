package com.optical.modules.inventory.controller;

import com.optical.modules.inventory.dto.InventoryPageResponse;
import com.optical.modules.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_USER')")
@Tag(name = "Inventory", description = "Branch inventory APIs")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public InventoryPageResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String lensSubType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return inventoryService.search(q, branchId, productType, lensSubType, page, size);
    }

    @GetMapping("/branches/{branchId}")
    public InventoryPageResponse getByBranch(
            @PathVariable Long branchId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String lensSubType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return inventoryService.getByBranch(branchId, q, productType, lensSubType, page, size);
    }
}
