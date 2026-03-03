package com.optical.modules.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "accessory_variant_details")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccessoryVariantDetails {

    @Id
    @Column(name = "variant_id")
    @EqualsAndHashCode.Include
    private Long variantId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "item_type", length = 50)
    private String itemType;
}


