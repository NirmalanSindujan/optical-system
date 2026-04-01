package com.optical.modules.billing.controller;

import com.optical.modules.billing.dto.BranchCollectionSummaryResponse;
import com.optical.modules.billing.dto.CustomerBillCreateRequest;
import com.optical.modules.billing.dto.CustomerBillPageResponse;
import com.optical.modules.billing.dto.CustomerBillResponse;
import com.optical.modules.billing.service.CustomerBillService;
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
@RequestMapping("/api/customer-bills")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_USER')")
@Tag(name = "Customer Bills", description = "Customer billing and branch sales collection APIs")
@SecurityRequirement(name = "bearerAuth")
public class CustomerBillController {

    private final CustomerBillService customerBillService;

    @PostMapping
    public CustomerBillResponse create(@Valid @RequestBody CustomerBillCreateRequest request) {
        return customerBillService.create(request);
    }

    @GetMapping("/{id}")
    public CustomerBillResponse getById(@PathVariable Long id) {
        return customerBillService.getById(id);
    }

    @GetMapping
    public CustomerBillPageResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return customerBillService.search(q, branchId, page, size);
    }

    @GetMapping("/branches/{branchId}/collection-summary")
    public BranchCollectionSummaryResponse getBranchCollectionSummary(@PathVariable Long branchId) {
        return customerBillService.getBranchCollectionSummary(branchId);
    }
}
