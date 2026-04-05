package com.optical.modules.expense.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExpensePageResponse {

    private List<ExpenseResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
