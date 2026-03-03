package com.optical.modules.product.entity;

import com.optical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(
        name = "product",
        indexes = {
                @Index(name = "idx_product_name", columnList = "name"),
                @Index(name = "idx_product_product_type_code", columnList = "product_type_code")
        }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@SQLDelete(sql = "UPDATE product SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_type_code", nullable = false, referencedColumnName = "code")
    private ProductType productType;

    @Column(name = "brand_name", length = 150)
    private String brandName;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @EqualsAndHashCode.Include
    private Long entityId() {
        return getId();
    }
}


