package com.optical.modules.billing.entity;

import com.optical.common.base.BaseEntity;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.customer.entity.Customer;
import com.optical.modules.patient.entity.Patient;
import com.optical.modules.prescription.entity.Prescription;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_bill")
@Getter
@Setter
public class CustomerBill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "bill_number", length = 100)
    private String billNumber;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "balance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "LKR";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "customerBill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerBillItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "customerBill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerBillPayment> payments = new ArrayList<>();

    @jakarta.persistence.OneToOne(mappedBy = "customerBill", cascade = CascadeType.ALL, orphanRemoval = true)
    private Prescription prescription;
}
