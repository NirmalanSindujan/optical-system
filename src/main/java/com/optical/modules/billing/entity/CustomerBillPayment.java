package com.optical.modules.billing.entity;

import com.optical.common.base.BaseEntity;
import com.optical.modules.purchase.entity.PaymentMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer_bill_payment")
@Getter
@Setter
public class CustomerBillPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_bill_id", nullable = false)
    private CustomerBill customerBill;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "cheque_number", length = 100)
    private String chequeNumber;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "cheque_bank_name", length = 150)
    private String chequeBankName;

    @Column(name = "cheque_branch_name", length = 150)
    private String chequeBranchName;

    @Column(name = "cheque_account_holder", length = 150)
    private String chequeAccountHolder;

    @Column(length = 150)
    private String reference;
}
