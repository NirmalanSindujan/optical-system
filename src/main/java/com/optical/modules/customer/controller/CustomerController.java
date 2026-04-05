package com.optical.modules.customer.controller;

import com.optical.common.base.PageResponse;
import com.optical.common.enums.ChequeStatus;
import com.optical.modules.customer.dto.CustomerChequeStatusUpdateRequest;
import com.optical.modules.customer.dto.CustomerPageResponse;
import com.optical.modules.customer.dto.CustomerPendingBillsResponse;
import com.optical.modules.customer.dto.CustomerPendingPaymentRequest;
import com.optical.modules.customer.dto.CustomerPendingPaymentResponse;
import com.optical.modules.customer.dto.CustomerReceivedChequeResponse;
import com.optical.modules.customer.dto.CustomerRequest;
import com.optical.modules.customer.dto.CustomerResponse;
import com.optical.modules.customer.dto.CustomerSummaryResponse;
import com.optical.modules.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @GetMapping
    public CustomerPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return customerService.search(q, page, size);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @GetMapping("/{id}/summary")
    public CustomerSummaryResponse getSummary(@PathVariable Long id) {
        return customerService.getSummary(id);
    }

    @GetMapping("/{id}/pending-bills")
    public CustomerPendingBillsResponse getPendingBills(@PathVariable Long id) {
        return customerService.getPendingBills(id);
    }

    @PostMapping("/{id}/pending-payments")
    public CustomerPendingPaymentResponse payPendingBills(
            @PathVariable Long id,
            @Valid @RequestBody CustomerPendingPaymentRequest request
    ) {
        return customerService.payPendingBills(id, request);
    }

    @GetMapping("/received-cheques")
    public PageResponse<CustomerReceivedChequeResponse> getReceivedCheques(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) ChequeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return customerService.getReceivedCheques(customerId, status, page, size);
    }

    @PatchMapping("/received-cheques/{paymentId}/status")
    public CustomerReceivedChequeResponse updateReceivedChequeStatus(
            @PathVariable Long paymentId,
            @Valid @RequestBody CustomerChequeStatusUpdateRequest request
    ) {
        return customerService.updateReceivedChequeStatus(paymentId, request);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }
}
