package com.optical.modules.supplier.dto;

import com.optical.common.enums.ChequeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierChequeStatusUpdateRequest {

    @NotNull
    private ChequeStatus expectedCurrentStatus;

    @NotNull
    private ChequeStatus newStatus;

    private String chequeBankName;
    private String chequeBranchName;
    private String chequeAccountHolder;
    private String notes;
}
