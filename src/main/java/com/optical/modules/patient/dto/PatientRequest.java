package com.optical.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PatientRequest {

    @NotBlank
    private String name;

    private String gender;

    private LocalDate dob;

    private String notes;
}
