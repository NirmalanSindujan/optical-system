package com.optical.modules.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CustomerResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String gender;
    private LocalDate dob;
    private String notes;
    private BigDecimal pendingAmount;
}
