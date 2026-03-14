package com.optical.modules.supplier.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SupplierSummaryResponse {
    private Long id;
    private String name;
}
