package com.optical.modules.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class BusinessSummaryResponse {

    private BigDecimal cashInHand;
    private BigDecimal bankBalance;
    private BigDecimal totalReceivable;
    private BigDecimal totalPending;
    private List<BranchCashSummaryResponse> branchCashInHand;
}
