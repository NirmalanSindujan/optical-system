package com.optical.modules.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CustomerPendingPaymentAllocationResponse {

    private Long billId;
    private String billNumber;
    private BigDecimal paidAmount;
    private BigDecimal remainingPendingAmount;
}
