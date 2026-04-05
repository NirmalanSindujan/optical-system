package com.optical.modules.expense.dto;

import com.optical.modules.expense.enums.RecurringType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ExpenseCategoryRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private RecurringType recurringType;

    private LocalDate reminderDate;
}
