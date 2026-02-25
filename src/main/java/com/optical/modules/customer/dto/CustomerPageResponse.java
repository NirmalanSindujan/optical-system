package com.optical.modules.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerPageResponse {

    private List<CustomerResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
