package com.optical.modules.product.entity;

import com.optical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_lot")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class InventoryLot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "purchase_cost", precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "LKR";

    @Column(name = "qty_received", nullable = false, precision = 12, scale = 2)
    private BigDecimal qtyReceived;

    @Column(name = "qty_remaining", nullable = false, precision = 12, scale = 2)
    private BigDecimal qtyRemaining;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @EqualsAndHashCode.Include
    private Long entityId() {
        return getId();
    }
}


