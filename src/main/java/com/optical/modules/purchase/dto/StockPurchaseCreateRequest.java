package com.optical.modules.purchase.dto;

import com.optical.modules.purchase.entity.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class StockPurchaseCreateRequest {

    @NotNull
    private Long supplierId;

    private Long branchId;

    @NotNull
    private LocalDate purchaseDate;

    private String billNumber;

    @NotNull
    private PaymentMode paymentMode;

    private BigDecimal paidAmount;

    private String currencyCode;

    private String notes;

    @Valid
    @NotEmpty
    private List<StockPurchaseItemRequest> items;
}
