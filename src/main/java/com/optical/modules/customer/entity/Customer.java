package com.optical.modules.customer.entity;

import com.optical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer")
@Getter
@Setter
public class Customer extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String gender;

    private LocalDate dob;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "pending_amount", precision = 12, scale = 2)
    private BigDecimal pendingAmount = BigDecimal.ZERO;
}
