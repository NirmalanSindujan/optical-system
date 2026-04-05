package com.optical.modules.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CustomerPendingBillResponse {

    private Long billId;
    private String billNumber;
    private LocalDate billDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private String currencyCode;
}
