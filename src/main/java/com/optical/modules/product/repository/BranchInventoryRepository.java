package com.optical.modules.product.repository;

import com.optical.modules.product.entity.BranchInventory;
import com.optical.modules.product.entity.BranchInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, BranchInventoryId> {
    java.util.Optional<BranchInventory> findByBranch_IdAndVariant_Id(Long branchId, Long variantId);
}


