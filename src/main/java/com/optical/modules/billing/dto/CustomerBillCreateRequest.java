package com.optical.modules.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CustomerBillCreateRequest {

    private Long customerId;

    @NotNull
    private Long branchId;

    private String billNumber;

    @NotNull
    private LocalDate billDate;

    private java.math.BigDecimal discountAmount;

    private String currencyCode;

    private String notes;

    @Valid
    @NotEmpty
    private List<CustomerBillItemRequest> items;

    @Valid
    @NotEmpty
    private List<CustomerBillPaymentRequest> payments;
}
