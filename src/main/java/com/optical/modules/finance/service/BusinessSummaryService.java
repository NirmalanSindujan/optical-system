package com.optical.modules.finance.service;

import com.optical.common.enums.ChequeStatus;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.billing.repository.CustomerBillPaymentRepository;
import com.optical.modules.billing.entity.CustomerBillPayment;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.expense.enums.ExpenseSource;
import com.optical.modules.expense.entity.Expense;
import com.optical.modules.expense.repository.ExpenseRepository;
import com.optical.modules.finance.dto.CashLedgerEntryDirection;
import com.optical.modules.finance.dto.CashLedgerEntryResponse;
import com.optical.modules.finance.dto.CashLedgerEntryType;
import com.optical.modules.finance.dto.CashLedgerResponse;
import com.optical.modules.finance.dto.BranchCashSummaryResponse;
import com.optical.modules.finance.dto.BusinessSummaryResponse;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.supplier.entity.SupplierPaymentAllocation;
import com.optical.modules.supplier.repository.SupplierPaymentAllocationRepository;
import com.optical.modules.supplier.repository.SupplierCreditLedgerRepository;
import com.optical.modules.supplier.repository.SupplierRepository;
import com.optical.modules.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessSummaryService {

    private final CustomerBillPaymentRepository customerBillPaymentRepository;
    private final SupplierCreditLedgerRepository supplierCreditLedgerRepository;
    private final SupplierPaymentAllocationRepository supplierPaymentAllocationRepository;
    private final ExpenseRepository expenseRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public BusinessSummaryResponse getSummary() {
        BigDecimal totalCashInflow = scale(customerBillPaymentRepository.sumCashCollections());
        BigDecimal totalBankInflow = scale(customerBillPaymentRepository.sumBankCollections());
        BigDecimal totalCashOutflow = scale(
                supplierCreditLedgerRepository.sumPaymentOutflowByPaymentMode(PaymentMode.CASH)
                        .add(scale(expenseRepository.sumAmountBySource(ExpenseSource.CASH)))
        );
        BigDecimal totalBankOutflow = scale(
                supplierCreditLedgerRepository.sumPaymentOutflowByPaymentMode(PaymentMode.BANK)
                        .add(scale(expenseRepository.sumAmountBySource(ExpenseSource.BANK)))
        );
        BigDecimal totalClearedSupplierCheques = scale(
                supplierCreditLedgerRepository.sumChequePaymentOutflowByStatus(ChequeStatus.CLEARED)
        );

        return BusinessSummaryResponse.builder()
                .cashInHand(scale(totalCashInflow.subtract(totalCashOutflow)))
                .bankBalance(scale(totalBankInflow.subtract(totalBankOutflow).subtract(totalClearedSupplierCheques)))
                .totalReceivable(scale(customerRepository.sumPendingAmount()))
                .totalPending(scale(supplierRepository.sumPendingAmount()))
                .branchCashInHand(branchRepository.findAll().stream()
                        .filter(branch -> branch.getDeletedAt() == null)
                        .map(branch -> BranchCashSummaryResponse.builder()
                                .branchId(branch.getId())
                                .branchCode(branch.getCode())
                                .branchName(branch.getName())
                                .cashInHand(scale(
                                        customerBillPaymentRepository.sumCashCollectionsByBranchId(branch.getId())
                                                .subtract(scale(expenseRepository.sumAmountByBranchIdAndSource(
                                                        branch.getId(),
                                                        ExpenseSource.CASH
                                                )))
                                ))
                                .build())
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public CashLedgerResponse getCashLedger(Long branchId, LocalDate fromDate, LocalDate toDate) {
        Branch branch = branchRepository.findByIdAndDeletedAtIsNull(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        List<CashLedgerEntryResponse> entries = new ArrayList<>();
        entries.addAll(loadCustomerCashEntries(branchId, fromDateTime, toDateTime).stream()
                .map(this::mapCustomerCashEntry)
                .toList());
        entries.addAll(loadExpenseCashEntries(branchId, fromDate, toDate).stream()
                .map(this::mapExpenseCashEntry)
                .toList());
        entries.addAll(loadSupplierCashEntries(branchId, fromDate, toDate).stream()
                .map(this::mapSupplierCashEntry)
                .toList());

        entries.sort(Comparator
                .comparing(CashLedgerEntryResponse::getTransactionDate)
                .thenComparing(CashLedgerEntryResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CashLedgerEntryResponse::getTransactionId));

        BigDecimal totalIncome = entries.stream()
                .filter(entry -> entry.getDirection() == CashLedgerEntryDirection.INCOME)
                .map(CashLedgerEntryResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutgoing = entries.stream()
                .filter(entry -> entry.getDirection() == CashLedgerEntryDirection.OUTGOING)
                .map(CashLedgerEntryResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CashLedgerResponse.builder()
                .branchId(branch.getId())
                .branchCode(branch.getCode())
                .branchName(branch.getName())
                .fromDate(fromDate)
                .toDate(toDate)
                .totalIncome(scale(totalIncome))
                .totalOutgoing(scale(totalOutgoing))
                .netCashMovement(scale(totalIncome.subtract(totalOutgoing)))
                .entries(entries)
                .build();
    }

    private List<Expense> loadExpenseCashEntries(Long branchId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return expenseRepository.findCashLedgerEntriesByBranchIdAndExpenseDateBetween(branchId, fromDate, toDate);
        }
        if (fromDate != null) {
            return expenseRepository.findCashLedgerEntriesByBranchIdAndExpenseDateGreaterThanEqual(branchId, fromDate);
        }
        if (toDate != null) {
            return expenseRepository.findCashLedgerEntriesByBranchIdAndExpenseDateLessThanEqual(branchId, toDate);
        }
        return expenseRepository.findCashLedgerEntriesByBranchId(branchId);
    }

    private List<CustomerBillPayment> loadCustomerCashEntries(Long branchId, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        if (fromDateTime != null && toDateTime != null) {
            return customerBillPaymentRepository.findCashLedgerEntriesByBranchIdAndCreatedAtBetween(branchId, fromDateTime, toDateTime);
        }
        if (fromDateTime != null) {
            return customerBillPaymentRepository.findCashLedgerEntriesByBranchIdAndCreatedAtGreaterThanEqual(branchId, fromDateTime);
        }
        if (toDateTime != null) {
            return customerBillPaymentRepository.findCashLedgerEntriesByBranchIdAndCreatedAtLessThan(branchId, toDateTime);
        }
        return customerBillPaymentRepository.findCashLedgerEntriesByBranchId(branchId);
    }

    private List<SupplierPaymentAllocation> loadSupplierCashEntries(Long branchId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return supplierPaymentAllocationRepository.findCashLedgerEntriesByBranchIdAndEntryDateBetween(branchId, fromDate, toDate);
        }
        if (fromDate != null) {
            return supplierPaymentAllocationRepository.findCashLedgerEntriesByBranchIdAndEntryDateGreaterThanEqual(branchId, fromDate);
        }
        if (toDate != null) {
            return supplierPaymentAllocationRepository.findCashLedgerEntriesByBranchIdAndEntryDateLessThanEqual(branchId, toDate);
        }
        return supplierPaymentAllocationRepository.findCashLedgerEntriesByBranchId(branchId);
    }

    private CashLedgerEntryResponse mapCustomerCashEntry(CustomerBillPayment payment) {
        String customerName = payment.getCustomerBill().getCustomer() == null
                ? null
                : payment.getCustomerBill().getCustomer().getName();
        String reference = payment.getReference() == null
                ? payment.getCustomerBill().getBillNumber()
                : payment.getReference();

        return CashLedgerEntryResponse.builder()
                .entryType(CashLedgerEntryType.CUSTOMER_BILL_PAYMENT)
                .direction(CashLedgerEntryDirection.INCOME)
                .transactionId(payment.getId())
                .transactionDate(payment.getCreatedAt() == null
                        ? payment.getCustomerBill().getBillDate()
                        : payment.getCreatedAt().toLocalDate())
                .createdAt(payment.getCreatedAt())
                .amount(scale(payment.getAmount()))
                .reference(reference)
                .description("Customer bill payment")
                .partyName(customerName)
                .build();
    }

    private CashLedgerEntryResponse mapExpenseCashEntry(Expense expense) {
        return CashLedgerEntryResponse.builder()
                .entryType(CashLedgerEntryType.EXPENSE)
                .direction(CashLedgerEntryDirection.OUTGOING)
                .transactionId(expense.getId())
                .transactionDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .amount(scale(expense.getAmount()))
                .reference(expense.getReference())
                .description(expense.getDescription() == null ? expense.getCategory().getName() : expense.getDescription())
                .partyName(expense.getCategory().getName())
                .build();
    }

    private CashLedgerEntryResponse mapSupplierCashEntry(SupplierPaymentAllocation allocation) {
        return CashLedgerEntryResponse.builder()
                .entryType(CashLedgerEntryType.SUPPLIER_PAYMENT)
                .direction(CashLedgerEntryDirection.OUTGOING)
                .transactionId(allocation.getId())
                .transactionDate(allocation.getSupplierCreditLedger().getEntryDate())
                .createdAt(allocation.getCreatedAt())
                .amount(scale(allocation.getAmount()))
                .reference(allocation.getSupplierCreditLedger().getReference())
                .description("Supplier cash payment allocation")
                .partyName(allocation.getSupplierCreditLedger().getSupplier().getName())
                .build();
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
