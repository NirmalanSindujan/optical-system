package com.optical.modules.product.entity;

import com.optical.modules.branch.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "branch_inventory")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BranchInventory {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private BranchInventoryId id = new BranchInventoryId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("branchId")
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("variantId")
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "on_hand", nullable = false, precision = 12, scale = 2)
    private BigDecimal onHand = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal reserved = BigDecimal.ZERO;

    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 2)
    private BigDecimal reorderLevel = BigDecimal.ZERO;
}


