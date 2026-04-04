package com.optical.modules.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CustomerSummaryResponse {

    private Long customerId;
    private String customerName;
    private BigDecimal pendingAmount;
    private long totalBills;
    private BigDecimal totalBilledAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalOutstandingAmount;
    private long totalPatients;
    private long totalPrescriptions;
}
