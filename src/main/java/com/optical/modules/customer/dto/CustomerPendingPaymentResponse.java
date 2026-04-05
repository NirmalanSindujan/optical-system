package com.optical.modules.customer.dto;

import com.optical.modules.purchase.entity.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CustomerPendingPaymentResponse {

    private Long customerId;
    private String customerName;
    private PaymentMode paymentMode;
    private BigDecimal amount;
    private String reference;
    private BigDecimal totalPendingAmount;
    private List<CustomerPendingPaymentAllocationResponse> allocations;
}
