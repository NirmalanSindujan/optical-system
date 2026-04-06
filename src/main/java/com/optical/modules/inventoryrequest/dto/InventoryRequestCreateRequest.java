package com.optical.modules.inventoryrequest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InventoryRequestCreateRequest {

    @NotNull
    private Long requestingBranchId;

    @NotNull
    private Long supplyingBranchId;

    private String requestNote;

    @Valid
    @NotEmpty
    private List<InventoryRequestCreateItem> items;
}
