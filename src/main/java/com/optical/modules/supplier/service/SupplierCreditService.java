package com.optical.modules.supplier.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.supplier.dto.SupplierCreditLedgerResponse;
import com.optical.modules.supplier.dto.SupplierCreditSummaryResponse;
import com.optical.modules.supplier.dto.SupplierPaymentRequest;
import com.optical.modules.supplier.entity.Supplier;
import com.optical.modules.supplier.entity.SupplierCreditEntryType;
import com.optical.modules.supplier.entity.SupplierCreditLedger;
import com.optical.modules.supplier.repository.SupplierCreditLedgerRepository;
import com.optical.modules.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class SupplierCreditService {

    private final SupplierRepository supplierRepository;
    private final SupplierCreditLedgerRepository supplierCreditLedgerRepository;

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

        supplier.setPendingAmount(scale(pendingAmount.subtract(amount)));

        SupplierCreditLedger ledger = new SupplierCreditLedger();
        ledger.setSupplier(supplier);
        ledger.setEntryDate(request.getPaymentDate());
        ledger.setAmount(scale(amount.negate()));
        ledger.setEntryType(SupplierCreditEntryType.PAYMENT);
        ledger.setPaymentMode(request.getPaymentMode());
        ledger.setReference(normalize(request.getReference()));
        ledger.setNotes(normalize(request.getNotes()));

        return mapLedger(supplierCreditLedgerRepository.save(ledger));
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
                .build();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
