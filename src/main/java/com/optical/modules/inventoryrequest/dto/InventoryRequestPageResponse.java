package com.optical.modules.inventoryrequest.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InventoryRequestPageResponse {
    private List<InventoryRequestResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
