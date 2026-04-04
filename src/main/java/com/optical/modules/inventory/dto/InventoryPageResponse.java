package com.optical.modules.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InventoryPageResponse {

    private List<InventoryItemResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
    private Long branchId;
}
