package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "Paginated response for sunglasses list")
public class SunglassesPageResponse {

    private List<SunglassesListResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
