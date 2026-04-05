package com.optical.modules.supplier.entity;

import com.optical.common.base.BaseEntity;
import com.optical.common.enums.ChequeStatus;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.purchase.entity.StockPurchase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier_credit_ledger")
@Getter
@Setter
public class SupplierCreditLedger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_purchase_id")
    private StockPurchase stockPurchase;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private SupplierCreditEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20)
    private PaymentMode paymentMode;

    @Column(length = 100)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String notes;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "cheque_status", length = 20)
    private ChequeStatus chequeStatus;

    @Column(name = "cheque_status_notes", columnDefinition = "TEXT")
    private String chequeStatusNotes;

    @Column(name = "cheque_status_changed_at")
    private LocalDateTime chequeStatusChangedAt;

    @OneToMany(mappedBy = "supplierCreditLedger")
    private List<SupplierPaymentAllocation> allocations = new ArrayList<>();
}
