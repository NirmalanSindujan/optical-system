package com.optical.modules.supplier.dto;

import com.optical.common.enums.ChequeStatus;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.supplier.entity.SupplierCreditEntryType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SupplierCreditLedgerResponse {

    private Long id;
    private LocalDate entryDate;
    private BigDecimal amount;
    private SupplierCreditEntryType entryType;
    private PaymentMode paymentMode;
    private Long branchId;
    private String branchName;
    private String reference;
    private String notes;
    private String chequeNumber;
    private LocalDate chequeDate;
    private String chequeBankName;
    private String chequeBranchName;
    private String chequeAccountHolder;
    private ChequeStatus chequeStatus;
    private String chequeStatusNotes;
    private Long stockPurchaseId;
    private List<SupplierPaymentAllocationResponse> allocations;
}
