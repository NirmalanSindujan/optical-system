package com.optical.modules.inventoryrequest.repository;

import com.optical.modules.inventoryrequest.entity.InventoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InventoryRequestRepository extends JpaRepository<InventoryRequest, Long>, JpaSpecificationExecutor<InventoryRequest> {

    @Override
    @EntityGraph(attributePaths = {
            "requestingBranch",
            "supplyingBranch",
            "requestedBy",
            "processedBy",
            "items",
            "items.variant",
            "items.variant.product",
            "items.variant.uom"
    })
    Page<InventoryRequest> findAll(Specification<InventoryRequest> spec, Pageable pageable);

    @EntityGraph(attributePaths = {
            "requestingBranch",
            "supplyingBranch",
            "requestedBy",
            "processedBy",
            "items",
            "items.variant",
            "items.variant.product",
            "items.variant.uom"
    })
    Optional<InventoryRequest> findByIdAndDeletedAtIsNull(Long id);
}
