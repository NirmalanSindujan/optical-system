package com.optical.modules.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BranchCollectionSummaryResponse {
    private Long branchId;
    private String branchName;
    private BigDecimal totalSales;
    private BigDecimal cashInHand;
    private BigDecimal universalBankBalance;
    private BigDecimal chequeCollections;
    private BigDecimal creditOutstanding;
}
