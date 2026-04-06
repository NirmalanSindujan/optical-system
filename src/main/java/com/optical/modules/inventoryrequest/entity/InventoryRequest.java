package com.optical.modules.inventoryrequest.entity;

import com.optical.common.base.BaseEntity;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.users.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_request")
@Getter
@Setter
public class InventoryRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requesting_branch_id", nullable = false)
    private Branch requestingBranch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplying_branch_id", nullable = false)
    private Branch supplyingBranch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private User processedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryRequestStatus status = InventoryRequestStatus.PENDING;

    @Column(name = "request_note", columnDefinition = "TEXT")
    private String requestNote;

    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "inventoryRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryRequestItem> items = new ArrayList<>();
}
