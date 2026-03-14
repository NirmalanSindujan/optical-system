package com.optical.modules.supplier.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SupplierPaymentAllocationResponse {

    private Long id;
    private Long stockPurchaseId;
    private String purchaseReference;
    private BigDecimal amount;
}
