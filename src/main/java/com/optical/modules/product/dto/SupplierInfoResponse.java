package com.optical.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Basic supplier information")
public class SupplierInfoResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
}
