package com.optical.modules.product.entity;

import jakarta.persistence.CascadeType;
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

import java.math.BigDecimal;

@Entity
@Table(name = "lens_variant_details")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LensVariantDetails {

    @Id
    @Column(name = "variant_id")
    @EqualsAndHashCode.Include
    private Long variantId;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(length = 100)
    private String material;

    @Column(name = "lens_sub_type", length = 30, nullable = false)
    private String lensSubType;

    @Column(name = "lens_index", precision = 4, scale = 2)
    private BigDecimal lensIndex;

    @Column(name = "lens_type", length = 30)
    private String lensType;

    @Column(name = "coating_code", length = 100)
    private String coatingCode;

    @Column(precision = 5, scale = 2)
    private BigDecimal sph;

    @Column(precision = 5, scale = 2)
    private BigDecimal cyl;

    @Column(name = "add_power", precision = 4, scale = 2)
    private BigDecimal addPower;

    @Column(length = 50)
    private String color;

    @Column(name = "base_curve", length = 50)
    private String baseCurve;
}


