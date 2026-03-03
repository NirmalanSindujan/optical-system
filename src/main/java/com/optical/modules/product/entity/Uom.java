package com.optical.modules.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "uom")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Uom {

    @Id
    @Column(length = 30, nullable = false)
    @EqualsAndHashCode.Include
    private String code;

    @Column(nullable = false, length = 100)
    private String name;
}


