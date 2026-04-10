package com.optical.modules.customer.dto;

import com.optical.modules.purchase.entity.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CustomerOpeningBalancePaymentResponse {

    private Long customerId;
    private String customerName;
    private PaymentMode paymentMode;
    private BigDecimal amount;
    private String reference;
    private BigDecimal totalPendingAmount;
}
