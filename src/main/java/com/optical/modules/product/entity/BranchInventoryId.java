package com.optical.modules.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class BranchInventoryId implements Serializable {

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "variant_id")
    private Long variantId;
}


