package com.optical.modules.customer.dto;

import com.optical.modules.purchase.entity.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CustomerPendingPaymentRequest {

    @NotNull
    private PaymentMode paymentMode;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String chequeNumber;
    private LocalDate chequeDate;
    private String chequeBankName;
    private String chequeBranchName;
    private String chequeAccountHolder;
    private String reference;

    @Valid
    @NotEmpty
    private List<CustomerPendingPaymentAllocationRequest> allocations;
}
