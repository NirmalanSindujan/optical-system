package com.optical.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank
    private String name;

    private String phone;

    private String email;

    private String address;

    private String gender;

    private LocalDate dob;

    private String notes;
}
