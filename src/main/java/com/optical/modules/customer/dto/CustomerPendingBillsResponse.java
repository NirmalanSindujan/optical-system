package com.optical.modules.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CustomerPendingBillsResponse {

    private Long customerId;
    private String customerName;
    private BigDecimal totalPendingAmount;
    private List<CustomerPendingBillResponse> customerBills;
}
