package com.optical.modules.customer.dto;

import com.optical.common.enums.ChequeStatus;
import com.optical.modules.purchase.entity.PaymentMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerChequeStatusUpdateRequest {

    @NotNull
    private ChequeStatus expectedCurrentStatus;

    @NotNull
    private ChequeStatus newStatus;

    private PaymentMode settlementMode;
    private String notes;

    @AssertTrue(message = "settlementMode must be CASH or BANK when marking a received cheque as CLEARED")
    public boolean isSettlementModeValid() {
        if (newStatus != ChequeStatus.CLEARED) {
            return true;
        }
        return settlementMode == PaymentMode.CASH || settlementMode == PaymentMode.BANK;
    }
}
