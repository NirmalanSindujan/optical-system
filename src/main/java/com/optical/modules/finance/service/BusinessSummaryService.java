package com.optical.modules.finance.service;

import com.optical.common.enums.ChequeStatus;
import com.optical.modules.billing.repository.CustomerBillPaymentRepository;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.finance.dto.BranchCashSummaryResponse;
import com.optical.modules.finance.dto.BusinessSummaryResponse;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.supplier.repository.SupplierCreditLedgerRepository;
import com.optical.modules.supplier.repository.SupplierRepository;
import com.optical.modules.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class BusinessSummaryService {

    private final CustomerBillPaymentRepository customerBillPaymentRepository;
    private final SupplierCreditLedgerRepository supplierCreditLedgerRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public BusinessSummaryResponse getSummary() {
        BigDecimal totalCashInflow = scale(customerBillPaymentRepository.sumCashCollections());
        BigDecimal totalBankInflow = scale(customerBillPaymentRepository.sumBankCollections());
        BigDecimal totalCashOutflow = scale(supplierCreditLedgerRepository.sumPaymentOutflowByPaymentMode(PaymentMode.CASH));
        BigDecimal totalBankOutflow = scale(supplierCreditLedgerRepository.sumPaymentOutflowByPaymentMode(PaymentMode.BANK));
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
                                .cashInHand(scale(customerBillPaymentRepository.sumCashCollectionsByBranchId(branch.getId())))
                                .build())
                        .toList())
                .build();
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
