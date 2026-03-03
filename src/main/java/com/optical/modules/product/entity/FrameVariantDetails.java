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
@Table(name = "frame_variant_details")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FrameVariantDetails {

    @Id
    @Column(name = "variant_id")
    @EqualsAndHashCode.Include
    private Long variantId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "frame_code", length = 100)
    private String frameCode;

    @Column(name = "frame_type", length = 100)
    private String frameType;

    @Column(length = 100)
    private String color;

    @Column(length = 50)
    private String size;
}


