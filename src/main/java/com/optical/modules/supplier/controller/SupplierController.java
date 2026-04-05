package com.optical.modules.supplier.controller;

import com.optical.common.base.PageResponse;
import com.optical.common.enums.ChequeStatus;
import com.optical.modules.supplier.dto.SupplierCreditLedgerResponse;
import com.optical.modules.supplier.dto.SupplierCreditSummaryResponse;
import com.optical.modules.supplier.dto.SupplierChequeStatusUpdateRequest;
import com.optical.modules.supplier.dto.SupplierPageResponse;
import com.optical.modules.supplier.dto.SupplierPaymentRequest;
import com.optical.modules.supplier.dto.SupplierProvidedChequeResponse;
import com.optical.modules.supplier.dto.SupplierProductStockResponse;
import com.optical.modules.supplier.dto.SupplierRequest;
import com.optical.modules.supplier.dto.SupplierResponse;
import com.optical.modules.supplier.dto.SuplierPendingBillsResponse;
import com.optical.modules.supplier.service.SupplierCreditService;
import com.optical.modules.supplier.service.SupplierService;
import com.optical.modules.purchase.service.StockPurchaseService;
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
    private final StockPurchaseService stockPurchaseService;

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


    @GetMapping("/{id}/pending-bills")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public SuplierPendingBillsResponse getPendingBills(@PathVariable Long id) {
        return stockPurchaseService.getPendingBillsBySupplier(id);
    }

    @GetMapping("/provided-cheques")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public PageResponse<SupplierProvidedChequeResponse> getProvidedCheques(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) ChequeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return supplierCreditService.getProvidedCheques(supplierId, status, page, size);
    }

    @PatchMapping("/provided-cheques/{ledgerId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public SupplierProvidedChequeResponse updateProvidedChequeStatus(
            @PathVariable Long ledgerId,
            @Valid @RequestBody SupplierChequeStatusUpdateRequest request
    ) {
        return supplierCreditService.updateProvidedChequeStatus(ledgerId, request);
    }
}
