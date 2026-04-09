package com.optical.modules.supplier.dto;

import com.optical.common.enums.ChequeStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SupplierProvidedChequeResponse {

    private Long ledgerId;
    private Long supplierId;
    private String supplierName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private Long branchId;
    private String branchName;
    private ChequeStatus chequeStatus;
    private String chequeNumber;
    private LocalDate chequeDate;
    private String chequeBankName;
    private String chequeBranchName;
    private String chequeAccountHolder;
    private String reference;
    private String notes;
    private String statusNotes;
    private LocalDateTime statusChangedAt;
    private LocalDateTime createdAt;
    private List<SupplierProvidedChequeAllocationResponse> allocations;
}
