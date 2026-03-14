package com.optical.modules.supplier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SupplierPaymentAllocationRequest {

    @NotNull
    private Long stockPurchaseId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}
