package com.optical.modules.customer.dto;

import com.optical.common.enums.ChequeStatus;
import com.optical.modules.purchase.entity.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CustomerReceivedChequeResponse {

    private Long paymentId;
    private Long customerId;
    private String customerName;
    private Long billId;
    private String billNumber;
    private LocalDate billDate;
    private BigDecimal amount;
    private ChequeStatus chequeStatus;
    private String chequeNumber;
    private LocalDate chequeDate;
    private String chequeBankName;
    private String chequeBranchName;
    private String chequeAccountHolder;
    private PaymentMode settlementMode;
    private String reference;
    private String statusNotes;
    private LocalDateTime statusChangedAt;
    private LocalDateTime createdAt;
}
