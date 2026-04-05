package com.optical.modules.expense.entity;

import com.optical.common.base.BaseEntity;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.expense.enums.ExpenseSource;
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
@Table(name = "expense")
@Getter
@Setter
public class Expense extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseSource source;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(length = 150)
    private String reference;
}
