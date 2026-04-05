package com.optical.modules.expense.dto;

import com.optical.modules.expense.enums.ExpenseSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ExpenseRequest {

    @NotNull
    private Long branchId;

    @NotNull
    private Long categoryId;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal amount;

    private String description;

    @NotNull
    private ExpenseSource source;

    @NotNull
    private LocalDate expenseDate;

    private String reference;
}
