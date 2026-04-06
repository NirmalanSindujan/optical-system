package com.optical.modules.inventoryrequest.dto;

import com.optical.modules.inventoryrequest.entity.InventoryRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InventoryRequestResponse {
    private Long id;
    private Long requestingBranchId;
    private String requestingBranchName;
    private Long supplyingBranchId;
    private String supplyingBranchName;
    private Long requestedByUserId;
    private String requestedByUsername;
    private Long processedByUserId;
    private String processedByUsername;
    private InventoryRequestStatus status;
    private String requestNote;
    private String decisionNote;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private List<InventoryRequestItemResponse> items;
}
