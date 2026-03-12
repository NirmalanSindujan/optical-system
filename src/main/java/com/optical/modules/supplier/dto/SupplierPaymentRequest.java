package com.optical.modules.supplier.dto;

import com.optical.modules.purchase.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SupplierPaymentRequest {

    @NotNull
    private LocalDate paymentDate;

    @NotNull
    private PaymentMode paymentMode;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String reference;
    private String notes;
}
