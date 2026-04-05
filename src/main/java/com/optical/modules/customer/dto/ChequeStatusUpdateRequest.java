package com.optical.modules.customer.dto;

import com.optical.common.enums.ChequeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChequeStatusUpdateRequest {

    @NotNull
    private ChequeStatus expectedCurrentStatus;

    @NotNull
    private ChequeStatus newStatus;

    private String notes;
}
