package com.optical.modules.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerBillPageResponse {
    private List<CustomerBillSummaryResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
