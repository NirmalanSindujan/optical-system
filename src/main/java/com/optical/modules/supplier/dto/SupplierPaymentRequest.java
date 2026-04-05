package com.optical.modules.supplier.dto;

import com.optical.modules.purchase.entity.PaymentMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SupplierPaymentRequest {

    @NotNull
    @Schema(example = "2026-04-05")
    private LocalDate paymentDate;

    @NotNull
    @Schema(description = "Supported supplier payment modes", allowableValues = {"CASH", "BANK", "CHEQUE"})
    private PaymentMode paymentMode;

    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "Required when paymentMode is CHEQUE", example = "CHK-000145")
    private String chequeNumber;
    @Schema(description = "Required when paymentMode is CHEQUE", example = "2026-04-10")
    private LocalDate chequeDate;
    @Schema(description = "Optional when paymentMode is CHEQUE", example = "Bank of Ceylon")
    private String chequeBankName;
    @Schema(example = "Maharagama")
    private String chequeBranchName;
    @Schema(example = "Eyedeal Vision (Pvt) Ltd")
    private String chequeAccountHolder;
    @Schema(example = "SUP-PAY-1024")
    private String reference;
    private String notes;
    private List<@Valid SupplierPaymentAllocationRequest> allocations;

    @AssertTrue(message = "chequeNumber and chequeDate are required for cheque payments")
    public boolean isChequeDetailsValid() {
        if (paymentMode != PaymentMode.CHEQUE) {
            return true;
        }
        return hasText(chequeNumber) && chequeDate != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
