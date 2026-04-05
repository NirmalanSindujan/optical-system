package com.optical.modules.expense.entity;

import com.optical.common.base.BaseEntity;
import com.optical.modules.expense.enums.RecurringType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "expense_category")
@Getter
@Setter
public class ExpenseCategory extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurring_type", nullable = false, length = 20)
    private RecurringType recurringType = RecurringType.NONE;

    @Column(name = "reminder_date")
    private LocalDate reminderDate;
}
