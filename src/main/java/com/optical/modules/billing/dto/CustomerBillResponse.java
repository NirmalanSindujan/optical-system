package com.optical.modules.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class CustomerBillResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private BigDecimal customerPendingAmount;
    private Long patientId;
    private String patientName;
    private Long branchId;
    private String branchName;
    private String billNumber;
    private LocalDate billDate;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String currencyCode;
    private String notes;
    private List<CustomerBillItemResponse> items;
    private List<CustomerBillPaymentResponse> payments;
    private com.optical.modules.prescription.dto.PrescriptionResponse prescription;
}
