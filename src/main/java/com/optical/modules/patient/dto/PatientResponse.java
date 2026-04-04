package com.optical.modules.patient.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PatientResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String name;
    private String gender;
    private LocalDate dob;
    private String notes;
}
