package com.optical.modules.prescription.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PrescriptionResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long patientId;
    private String patientName;
    private Long customerBillId;
    private String billNumber;
    private LocalDate prescriptionDate;
    private JsonNode values;
    private String notes;
}
