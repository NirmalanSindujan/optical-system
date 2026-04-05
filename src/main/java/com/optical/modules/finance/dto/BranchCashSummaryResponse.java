package com.optical.modules.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BranchCashSummaryResponse {

    private Long branchId;
    private String branchCode;
    private String branchName;
    private BigDecimal cashInHand;
}
