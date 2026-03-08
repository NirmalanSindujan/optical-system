package com.optical.modules.product.entity;

import com.optical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

@Entity
@Table(
        name = "product_variant",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_variant_sku", columnNames = "sku"),
                @UniqueConstraint(name = "uk_product_variant_barcode", columnNames = "barcode")
        },
        indexes = {
                @Index(name = "idx_product_variant_product_id", columnList = "product_id"),
                @Index(name = "idx_product_variant_uom_code", columnList = "uom_code"),
                @Index(name = "idx_product_variant_purchase_price", columnList = "purchase_price"),
                @Index(name = "idx_product_variant_selling_price", columnList = "selling_price"),
                @Index(name = "idx_product_variant_quantity", columnList = "quantity")
        }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@SQLDelete(sql = "UPDATE product_variant SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(length = 100)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uom_code", nullable = false, referencedColumnName = "code")
    private Uom uom;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @EqualsAndHashCode.Include
    private Long entityId() {
        return getId();
    }
}


