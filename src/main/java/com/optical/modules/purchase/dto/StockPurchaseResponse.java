package com.optical.modules.purchase.dto;

import com.optical.modules.purchase.entity.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class StockPurchaseResponse {

    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long branchId;
    private String branchName;
    private String billNumber;
    private LocalDate purchaseDate;
    private PaymentMode paymentMode;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String currencyCode;
    private String notes;
    private BigDecimal supplierPendingAmount;
    private List<StockPurchaseItemResponse> items;
}
