package com.optical.modules.patient.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PatientPageResponse {
    private List<PatientResponse> items;
    private long totalCounts;
    private int page;
    private int size;
    private int totalPages;
}
