package com.optical.modules.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class CashLedgerResponse {

    private Long branchId;
    private String branchCode;
    private String branchName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalIncome;
    private BigDecimal totalOutgoing;
    private BigDecimal netCashMovement;
    private List<CashLedgerEntryResponse> entries;
}
