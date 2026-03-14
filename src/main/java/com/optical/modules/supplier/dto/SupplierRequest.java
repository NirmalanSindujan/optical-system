package com.optical.modules.supplier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SupplierRequest {

    @NotBlank
    private String name;

    private String phone;
    private String email;
    private String address;
    private String notes;
    @DecimalMin(value = "0.00")
    private BigDecimal pendingAmount;
    private LocalDate openingBalanceDate;
    private String openingBalanceReference;
    private String openingBalanceNotes;
}
