package com.optical.modules.billing.dto;

import com.optical.common.enums.ChequeStatus;
import com.optical.modules.purchase.entity.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CustomerBillPaymentResponse {
    private Long id;
    private PaymentMode paymentMode;
    private BigDecimal amount;
    private String chequeNumber;
    private LocalDate chequeDate;
    private String chequeBankName;
    private String chequeBranchName;
    private String chequeAccountHolder;
    private PaymentMode chequeSettlementMode;
    private String reference;
    private ChequeStatus chequeStatus;
    private String chequeStatusNotes;
}
