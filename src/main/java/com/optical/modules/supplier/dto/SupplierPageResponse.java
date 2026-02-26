package com.optical.modules.supplier.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SupplierPageResponse {

    private List<SupplierResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
