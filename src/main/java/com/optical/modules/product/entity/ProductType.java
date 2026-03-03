package com.optical.modules.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_type")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductType {

    @Id
    @Column(length = 50, nullable = false)
    @EqualsAndHashCode.Include
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_code", nullable = false, referencedColumnName = "code")
    private Category category;
}


