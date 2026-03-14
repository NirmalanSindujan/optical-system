package com.optical.modules.supplier.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.purchase.entity.StockPurchase;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.purchase.repository.StockPurchaseRepository;
import com.optical.modules.supplier.dto.SupplierCreditLedgerResponse;
import com.optical.modules.supplier.dto.SupplierPaymentAllocationRequest;
import com.optical.modules.supplier.dto.SupplierPaymentAllocationResponse;
import com.optical.modules.supplier.dto.SupplierCreditSummaryResponse;
import com.optical.modules.supplier.dto.SupplierPaymentRequest;
import com.optical.modules.supplier.entity.Supplier;
import com.optical.modules.supplier.entity.SupplierCreditEntryType;
import com.optical.modules.supplier.entity.SupplierCreditLedger;
import com.optical.modules.supplier.entity.SupplierPaymentAllocation;
import com.optical.modules.supplier.repository.SupplierCreditLedgerRepository;
import com.optical.modules.supplier.repository.SupplierPaymentAllocationRepository;
import com.optical.modules.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class SupplierCreditService {

    private final SupplierRepository supplierRepository;
    private final SupplierCreditLedgerRepository supplierCreditLedgerRepository;
    private final SupplierPaymentAllocationRepository supplierPaymentAllocationRepository;
    private final StockPurchaseRepository stockPurchaseRepository;

    @Transactional(readOnly = true)
    public SupplierCreditSummaryResponse getSummary(Long supplierId) {
        Supplier supplier = findSupplier(supplierId);
        return SupplierCreditSummaryResponse.builder()
                .supplierId(supplier.getId())
                .supplierName(supplier.getName())
                .pendingAmount(supplier.getPendingAmount())
                .entries(supplierCreditLedgerRepository.findBySupplierIdOrderByEntryDateDescIdDesc(supplierId).stream()
                        .map(this::mapLedger)
                        .toList())
                .build();
    }

    @Transactional
    public SupplierCreditLedgerResponse recordPayment(Long supplierId, SupplierPaymentRequest request) {
        Supplier supplier = findSupplier(supplierId);
        if (request.getPaymentMode() == PaymentMode.CREDIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier payment mode cannot be CREDIT");
        }

        BigDecimal amount = scale(request.getAmount());
        BigDecimal pendingAmount = zeroIfNull(supplier.getPendingAmount());
        if (amount.compareTo(pendingAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount cannot exceed supplier pending amount");
        }

        List<SupplierPaymentAllocationRequest> allocationRequests = request.getAllocations() == null
                ? List.of()
                : request.getAllocations();
        BigDecimal allocatedAmount = allocationRequests.stream()
                .map(SupplierPaymentAllocationRequest::getAmount)
                .map(this::scale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocatedAmount.compareTo(amount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocated amount cannot exceed payment amount");
        }

        supplier.setPendingAmount(scale(pendingAmount.subtract(amount)));

        SupplierCreditLedger ledger = new SupplierCreditLedger();
        ledger.setSupplier(supplier);
        ledger.setEntryDate(request.getPaymentDate());
        ledger.setAmount(scale(amount.negate()));
        ledger.setEntryType(SupplierCreditEntryType.PAYMENT);
        ledger.setPaymentMode(request.getPaymentMode());
        ledger.setReference(normalize(request.getReference()));
        ledger.setNotes(normalize(request.getNotes()));
        SupplierCreditLedger savedLedger = supplierCreditLedgerRepository.save(ledger);
        applyAllocations(savedLedger, supplier, allocationRequests);
        return mapLedger(savedLedger);
    }

    private Supplier findSupplier(Long supplierId) {
        return supplierRepository.findByIdAndDeletedAtIsNull(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

    private SupplierCreditLedgerResponse mapLedger(SupplierCreditLedger ledger) {
        return SupplierCreditLedgerResponse.builder()
                .id(ledger.getId())
                .entryDate(ledger.getEntryDate())
                .amount(ledger.getAmount())
                .entryType(ledger.getEntryType())
                .paymentMode(ledger.getPaymentMode())
                .reference(ledger.getReference())
                .notes(ledger.getNotes())
                .stockPurchaseId(ledger.getStockPurchase() == null ? null : ledger.getStockPurchase().getId())
                .allocations(ledger.getAllocations().stream().map(this::mapAllocation).toList())
                .build();
    }

    private void applyAllocations(
            SupplierCreditLedger ledger,
            Supplier supplier,
            List<SupplierPaymentAllocationRequest> allocationRequests
    ) {
        if (allocationRequests.isEmpty()) {
            return;
        }

        Map<Long, SupplierPaymentAllocationRequest> requestByPurchaseId = new LinkedHashMap<>();
        for (SupplierPaymentAllocationRequest allocationRequest : allocationRequests) {
            if (requestByPurchaseId.putIfAbsent(allocationRequest.getStockPurchaseId(), allocationRequest) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate stockPurchaseId allocations are not allowed");
            }
        }

        Map<Long, StockPurchase> purchasesById = new LinkedHashMap<>();
        for (StockPurchase stockPurchase : stockPurchaseRepository.findAllById(requestByPurchaseId.keySet())) {
            purchasesById.put(stockPurchase.getId(), stockPurchase);
        }

        List<SupplierPaymentAllocation> allocations = new ArrayList<>();
        for (Map.Entry<Long, SupplierPaymentAllocationRequest> entry : requestByPurchaseId.entrySet()) {
            Long purchaseId = entry.getKey();
            StockPurchase stockPurchase = purchasesById.get(purchaseId);
            if (stockPurchase == null || stockPurchase.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Stock purchase not found: " + purchaseId);
            }
            if (!stockPurchase.getSupplier().getId().equals(supplier.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock purchase does not belong to supplier: " + purchaseId);
            }

            BigDecimal allocationAmount = scale(entry.getValue().getAmount());
            BigDecimal balanceAmount = zeroIfNull(stockPurchase.getBalanceAmount());
            if (allocationAmount.compareTo(balanceAmount) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Allocation exceeds purchase balance for stock purchase: " + purchaseId
                );
            }

            stockPurchase.setPaidAmount(scale(zeroIfNull(stockPurchase.getPaidAmount()).add(allocationAmount)));
            stockPurchase.setBalanceAmount(scale(balanceAmount.subtract(allocationAmount)));

            SupplierPaymentAllocation allocation = new SupplierPaymentAllocation();
            allocation.setSupplierCreditLedger(ledger);
            allocation.setStockPurchase(stockPurchase);
            allocation.setAmount(allocationAmount);
            allocations.add(allocation);
        }

        supplierPaymentAllocationRepository.saveAll(allocations);
        ledger.setAllocations(allocations);
    }

    private SupplierPaymentAllocationResponse mapAllocation(SupplierPaymentAllocation allocation) {
        StockPurchase stockPurchase = allocation.getStockPurchase();
        return SupplierPaymentAllocationResponse.builder()
                .id(allocation.getId())
                .stockPurchaseId(stockPurchase.getId())
                .purchaseReference(stockPurchase.getBillNumber() == null ? "PURCHASE-" + stockPurchase.getId() : stockPurchase.getBillNumber())
                .amount(allocation.getAmount())
                .build();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
