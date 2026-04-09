package com.optical.modules.supplier.service;

import com.optical.common.base.PageResponse;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.common.enums.ChequeStatus;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.purchase.entity.StockPurchase;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.purchase.repository.StockPurchaseRepository;
import com.optical.modules.supplier.dto.SupplierCreditLedgerResponse;
import com.optical.modules.supplier.dto.SupplierChequeStatusUpdateRequest;
import com.optical.modules.supplier.dto.SupplierPaymentAllocationRequest;
import com.optical.modules.supplier.dto.SupplierPaymentAllocationResponse;
import com.optical.modules.supplier.dto.SupplierCreditSummaryResponse;
import com.optical.modules.supplier.dto.SupplierPaymentRequest;
import com.optical.modules.supplier.dto.SupplierProvidedChequeAllocationResponse;
import com.optical.modules.supplier.dto.SupplierProvidedChequeResponse;
import com.optical.modules.supplier.entity.Supplier;
import com.optical.modules.supplier.entity.SupplierCreditEntryType;
import com.optical.modules.supplier.entity.SupplierCreditLedger;
import com.optical.modules.supplier.entity.SupplierPaymentAllocation;
import com.optical.modules.supplier.repository.SupplierCreditLedgerRepository;
import com.optical.modules.supplier.repository.SupplierPaymentAllocationRepository;
import com.optical.modules.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class SupplierCreditService {

    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
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
        boolean chequePayment = request.getPaymentMode() == PaymentMode.CHEQUE;
        if (chequePayment) {
            validateChequeDetails(request);
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
        Branch branch = resolveBranch(request.getBranchId());

        SupplierCreditLedger ledger = new SupplierCreditLedger();
        ledger.setSupplier(supplier);
        ledger.setBranch(branch);
        ledger.setEntryDate(request.getPaymentDate());
        ledger.setAmount(scale(amount.negate()));
        ledger.setEntryType(SupplierCreditEntryType.PAYMENT);
        ledger.setPaymentMode(request.getPaymentMode());
        ledger.setChequeNumber(chequePayment ? normalize(request.getChequeNumber()) : null);
        ledger.setChequeDate(chequePayment ? request.getChequeDate() : null);
        ledger.setChequeBankName(chequePayment ? normalize(request.getChequeBankName()) : null);
        ledger.setChequeBranchName(chequePayment ? normalize(request.getChequeBranchName()) : null);
        ledger.setChequeAccountHolder(chequePayment ? normalize(request.getChequeAccountHolder()) : null);
        if (chequePayment) {
            ledger.setChequeStatus(ChequeStatus.PENDING);
            ledger.setChequeStatusChangedAt(LocalDateTime.now());
        }
        ledger.setReference(resolvePaymentReference(request.getReference()));
        ledger.setNotes(normalize(request.getNotes()));
        SupplierCreditLedger savedLedger = supplierCreditLedgerRepository.save(ledger);
        applyAllocations(savedLedger, supplier, allocationRequests);
        return mapLedger(savedLedger);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierProvidedChequeResponse> getProvidedCheques(Long supplierId, ChequeStatus status, int page, int size) {
        Page<SupplierCreditLedger> result = supplierCreditLedgerRepository.findProvidedCheques(
                supplierId,
                status,
                PageRequest.of(page, size)
        );

        return PageResponse.<SupplierProvidedChequeResponse>builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .content(result.getContent().stream().map(this::mapProvidedCheque).toList())
                .build();
    }

    @Transactional
    public SupplierProvidedChequeResponse updateProvidedChequeStatus(Long ledgerId, SupplierChequeStatusUpdateRequest request) {
        SupplierCreditLedger ledger = supplierCreditLedgerRepository.findActiveByIdForUpdate(ledgerId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier cheque payment not found"));

        if (ledger.getEntryType() != SupplierCreditEntryType.PAYMENT || ledger.getPaymentMode() != PaymentMode.CHEQUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ledger entry is not a cheque payment");
        }

        ChequeStatus currentStatus = ledger.getChequeStatus();
        if (currentStatus != request.getExpectedCurrentStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cheque status has changed. Refresh and try again");
        }

        ChequeStatus newStatus = request.getNewStatus();
        if (currentStatus != newStatus) {
            boolean currentApplied = isFinanciallyApplied(currentStatus);
            boolean nextApplied = isFinanciallyApplied(newStatus);
            if (currentApplied && !nextApplied) {
                reverseProvidedCheque(ledger);
            } else if (!currentApplied && nextApplied) {
                applyProvidedCheque(ledger);
            }
        }

        ledger.setChequeStatus(newStatus);
        if (newStatus == ChequeStatus.CLEARED) {
            ledger.setChequeBankName(normalize(request.getChequeBankName()));
            ledger.setChequeBranchName(normalize(request.getChequeBranchName()));
            ledger.setChequeAccountHolder(normalize(request.getChequeAccountHolder()));
        }
        ledger.setChequeStatusNotes(normalize(request.getNotes()));
        ledger.setChequeStatusChangedAt(LocalDateTime.now());
        return mapProvidedCheque(ledger);
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
                .branchId(ledger.getBranch() == null ? null : ledger.getBranch().getId())
                .branchName(ledger.getBranch() == null ? null : ledger.getBranch().getName())
                .reference(ledger.getReference())
                .notes(ledger.getNotes())
                .chequeNumber(ledger.getChequeNumber())
                .chequeDate(ledger.getChequeDate())
                .chequeBankName(ledger.getChequeBankName())
                .chequeBranchName(ledger.getChequeBranchName())
                .chequeAccountHolder(ledger.getChequeAccountHolder())
                .chequeStatus(ledger.getChequeStatus())
                .chequeStatusNotes(ledger.getChequeStatusNotes())
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

    private void validateChequeDetails(SupplierPaymentRequest request) {
        if (normalize(request.getChequeNumber()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chequeNumber is required for cheque payments");
        }
        if (request.getChequeDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chequeDate is required for cheque payments");
        }
    }

    private String resolvePaymentReference(String reference) {
        String normalizedReference = normalize(reference);
        return normalizedReference == null ? "SUPPLIER-PAYMENT" : normalizedReference;
    }

    private SupplierProvidedChequeResponse mapProvidedCheque(SupplierCreditLedger ledger) {
        Supplier supplier = ledger.getSupplier();
        return SupplierProvidedChequeResponse.builder()
                .ledgerId(ledger.getId())
                .supplierId(supplier == null ? null : supplier.getId())
                .supplierName(supplier == null ? null : supplier.getName())
                .paymentDate(ledger.getEntryDate())
                .amount(scale(ledger.getAmount().abs()))
                .branchId(ledger.getBranch() == null ? null : ledger.getBranch().getId())
                .branchName(ledger.getBranch() == null ? null : ledger.getBranch().getName())
                .chequeStatus(ledger.getChequeStatus())
                .chequeNumber(ledger.getChequeNumber())
                .chequeDate(ledger.getChequeDate())
                .chequeBankName(ledger.getChequeBankName())
                .chequeBranchName(ledger.getChequeBranchName())
                .chequeAccountHolder(ledger.getChequeAccountHolder())
                .reference(ledger.getReference())
                .notes(ledger.getNotes())
                .statusNotes(ledger.getChequeStatusNotes())
                .statusChangedAt(ledger.getChequeStatusChangedAt())
                .createdAt(ledger.getCreatedAt())
                .allocations(ledger.getAllocations().stream().map(this::mapProvidedAllocation).toList())
                .build();
    }

    private SupplierProvidedChequeAllocationResponse mapProvidedAllocation(SupplierPaymentAllocation allocation) {
        StockPurchase stockPurchase = allocation.getStockPurchase();
        return SupplierProvidedChequeAllocationResponse.builder()
                .stockPurchaseId(stockPurchase.getId())
                .purchaseReference(stockPurchase.getBillNumber() == null ? "PURCHASE-" + stockPurchase.getId() : stockPurchase.getBillNumber())
                .amount(allocation.getAmount())
                .build();
    }

    private boolean isFinanciallyApplied(ChequeStatus status) {
        return status == ChequeStatus.PENDING || status == ChequeStatus.CLEARED;
    }

    private Branch resolveBranch(Long branchId) {
        if (branchId != null) {
            return branchRepository.findByIdAndDeletedAtIsNull(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        }
        return branchRepository.findFirstByIsMainTrueAndDeletedAtIsNull()
                .orElseThrow(() -> new ResourceNotFoundException("Main branch not found"));
    }

    private void reverseProvidedCheque(SupplierCreditLedger ledger) {
        Supplier supplier = ledger.getSupplier();
        BigDecimal amount = scale(ledger.getAmount().abs());

        supplier.setPendingAmount(scale(zeroIfNull(supplier.getPendingAmount()).add(amount)));

        for (SupplierPaymentAllocation allocation : ledger.getAllocations()) {
            StockPurchase stockPurchase = allocation.getStockPurchase();
            BigDecimal allocationAmount = scale(allocation.getAmount());
            BigDecimal paidAmount = scale(zeroIfNull(stockPurchase.getPaidAmount()));
            if (allocationAmount.compareTo(paidAmount) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Purchase paid amount is too low to reject this cheque: " + stockPurchase.getId()
                );
            }

            stockPurchase.setPaidAmount(scale(paidAmount.subtract(allocationAmount)));
            stockPurchase.setBalanceAmount(scale(zeroIfNull(stockPurchase.getBalanceAmount()).add(allocationAmount)));
        }

        SupplierCreditLedger adjustment = new SupplierCreditLedger();
        adjustment.setSupplier(supplier);
        adjustment.setEntryDate(LocalDate.now());
        adjustment.setAmount(amount);
        adjustment.setEntryType(SupplierCreditEntryType.ADJUSTMENT);
        adjustment.setReference("CHEQUE-REJECTED:" + ledger.getId());
        adjustment.setNotes("Cheque rejected adjustment");
        supplierCreditLedgerRepository.save(adjustment);
    }

    private void applyProvidedCheque(SupplierCreditLedger ledger) {
        Supplier supplier = ledger.getSupplier();
        BigDecimal amount = scale(ledger.getAmount().abs());
        BigDecimal supplierPendingAmount = scale(zeroIfNull(supplier.getPendingAmount()));
        if (amount.compareTo(supplierPendingAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier pending amount is too low to re-apply this cheque");
        }

        for (SupplierPaymentAllocation allocation : ledger.getAllocations()) {
            StockPurchase stockPurchase = allocation.getStockPurchase();
            BigDecimal allocationAmount = scale(allocation.getAmount());
            BigDecimal balanceAmount = scale(zeroIfNull(stockPurchase.getBalanceAmount()));
            if (allocationAmount.compareTo(balanceAmount) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Purchase pending amount is too low to re-apply this cheque: " + stockPurchase.getId()
                );
            }

            stockPurchase.setPaidAmount(scale(zeroIfNull(stockPurchase.getPaidAmount()).add(allocationAmount)));
            stockPurchase.setBalanceAmount(scale(balanceAmount.subtract(allocationAmount)));
        }

        supplier.setPendingAmount(scale(supplierPendingAmount.subtract(amount)));

        SupplierCreditLedger adjustment = new SupplierCreditLedger();
        adjustment.setSupplier(supplier);
        adjustment.setEntryDate(LocalDate.now());
        adjustment.setAmount(scale(amount.negate()));
        adjustment.setEntryType(SupplierCreditEntryType.ADJUSTMENT);
        adjustment.setReference("CHEQUE-REAPPLIED:" + ledger.getId());
        adjustment.setNotes("Cheque status reversed adjustment");
        supplierCreditLedgerRepository.save(adjustment);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
