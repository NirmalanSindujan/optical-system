package com.optical.modules.expense.dto;

import com.optical.modules.expense.enums.ExpenseSource;
import com.optical.modules.expense.enums.RecurringType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ExpenseResponse {

    private Long id;
    private Long branchId;
    private String branchName;
    private Long categoryId;
    private String categoryName;
    private RecurringType categoryRecurringType;
    private LocalDate categoryReminderDate;
    private BigDecimal amount;
    private String description;
    private ExpenseSource source;
    private LocalDate expenseDate;
    private String reference;
    private LocalDateTime createdAt;
}
