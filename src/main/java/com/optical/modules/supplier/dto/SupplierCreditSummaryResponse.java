package com.optical.modules.supplier.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class SupplierCreditSummaryResponse {

    private Long supplierId;
    private String supplierName;
    private BigDecimal pendingAmount;
    private List<SupplierCreditLedgerResponse> entries;
}
