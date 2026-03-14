package com.optical.modules.supplier.entity;

import com.optical.common.base.BaseEntity;
import com.optical.modules.purchase.entity.StockPurchase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "supplier_payment_allocation")
@Getter
@Setter
public class SupplierPaymentAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_credit_ledger_id", nullable = false)
    private SupplierCreditLedger supplierCreditLedger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_purchase_id", nullable = false)
    private StockPurchase stockPurchase;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
