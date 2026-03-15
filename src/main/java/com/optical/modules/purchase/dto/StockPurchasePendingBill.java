package com.optical.modules.purchase.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class StockPurchasePendingBill {

    private Long purchaseId;
    private String billNumber;
    private LocalDate purchaseDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private String currencyCode;
    private String notes;
}
