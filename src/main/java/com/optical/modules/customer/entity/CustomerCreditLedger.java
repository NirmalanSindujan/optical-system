package com.optical.modules.customer.entity;

import com.optical.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_credit_ledger")
@Getter
@Setter
public class CustomerCreditLedger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType;

    @Column(length = 100)
    private String reference;
}
