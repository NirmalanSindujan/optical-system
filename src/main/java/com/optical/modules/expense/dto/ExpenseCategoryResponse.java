package com.optical.modules.expense.dto;

import com.optical.modules.expense.enums.RecurringType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ExpenseCategoryResponse {

    private Long id;
    private String name;
    private String description;
    private RecurringType recurringType;
    private LocalDate reminderDate;
}
