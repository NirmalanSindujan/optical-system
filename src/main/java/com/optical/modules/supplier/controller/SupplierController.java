package com.optical.modules.supplier.controller;

import com.optical.modules.supplier.dto.SupplierPageResponse;
import com.optical.modules.supplier.dto.SupplierCreditLedgerResponse;
import com.optical.modules.supplier.dto.SupplierProductStockResponse;
import com.optical.modules.supplier.dto.SupplierCreditSummaryResponse;
import com.optical.modules.supplier.dto.SupplierPaymentRequest;
import com.optical.modules.supplier.dto.SupplierRequest;
import com.optical.modules.supplier.dto.SupplierResponse;
import com.optical.modules.supplier.service.SupplierCreditService;
import com.optical.modules.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierCreditService supplierCreditService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
        return supplierService.create(request);
    }

    @GetMapping
    public SupplierPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return supplierService.search(q, page, size);
    }

    @GetMapping("/{id}")
    public SupplierResponse getById(@PathVariable Long id) {
        return supplierService.getById(id);
    }

    @GetMapping("/{id}/products")
    public List<SupplierProductStockResponse> getProducts(@PathVariable Long id) {
        return supplierService.getProducts(id);
    }

    @GetMapping("/{id}/credits")
    public SupplierCreditSummaryResponse getCreditSummary(@PathVariable Long id) {
        return supplierCreditService.getSummary(id);
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public SupplierCreditLedgerResponse recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody SupplierPaymentRequest request
    ) {
        return supplierCreditService.recordPayment(id, request);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        supplierService.delete(id);
    }
}
