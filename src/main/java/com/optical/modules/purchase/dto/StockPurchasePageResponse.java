package com.optical.modules.purchase.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StockPurchasePageResponse {

    private List<StockPurchaseResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
