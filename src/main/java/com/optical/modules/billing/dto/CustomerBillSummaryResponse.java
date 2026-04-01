package com.optical.modules.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CustomerBillSummaryResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long branchId;
    private String branchName;
    private String billNumber;
    private LocalDate billDate;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String currencyCode;
}
