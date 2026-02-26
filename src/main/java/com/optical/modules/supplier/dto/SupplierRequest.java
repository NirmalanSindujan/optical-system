package com.optical.modules.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierRequest {

    @NotBlank
    private String name;

    private String phone;
    private String email;
    private String address;
    private String notes;
}
