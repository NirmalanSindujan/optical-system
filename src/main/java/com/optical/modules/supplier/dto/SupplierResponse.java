package com.optical.modules.supplier.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SupplierResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private BigDecimal pendingAmount;
}
