package com.optical.modules.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CashLedgerEntryResponse {

    private CashLedgerEntryType entryType;
    private CashLedgerEntryDirection direction;
    private Long transactionId;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
    private BigDecimal amount;
    private String reference;
    private String description;
    private String partyName;
}
