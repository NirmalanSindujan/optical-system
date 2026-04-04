package com.optical.modules.prescription.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PrescriptionRequest {

    private LocalDate prescriptionDate;
    private JsonNode values;
    private String notes;
}
