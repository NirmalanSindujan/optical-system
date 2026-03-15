package com.optical.modules.supplier.dto;

import com.optical.modules.purchase.dto.StockPurchasePendingBill;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class SuplierPendingBillsResponse {

    private Long supplierId;
    private String supplierName;
    private BigDecimal totalPendingAmount;
    private List<StockPurchasePendingBill> supplierBills;
}
