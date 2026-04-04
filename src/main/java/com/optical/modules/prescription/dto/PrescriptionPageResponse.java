package com.optical.modules.prescription.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PrescriptionPageResponse {
    private List<PrescriptionResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
