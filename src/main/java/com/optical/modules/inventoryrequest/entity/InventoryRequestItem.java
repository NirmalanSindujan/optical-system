package com.optical.modules.inventoryrequest.entity;

import com.optical.common.base.BaseEntity;
import com.optical.modules.product.entity.ProductVariant;
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
@Table(name = "inventory_request_item")
@Getter
@Setter
public class InventoryRequestItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_request_id", nullable = false)
    private InventoryRequest inventoryRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "requested_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedQuantity;
}
