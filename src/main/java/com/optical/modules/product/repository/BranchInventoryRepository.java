package com.optical.modules.product.repository;

import com.optical.modules.product.entity.BranchInventory;
import com.optical.modules.product.entity.BranchInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, BranchInventoryId> {
    java.util.Optional<BranchInventory> findByBranch_IdAndVariant_Id(Long branchId, Long variantId);

    @Query("""
            select bi
            from BranchInventory bi
            join fetch bi.branch b
            join fetch bi.variant v
            join fetch v.product p
            join fetch v.uom u
            where b.deletedAt is null
              and (:branchId is null or b.id = :branchId)
            order by b.name asc, p.name asc, v.id asc
            """)
    List<BranchInventory> findInventoryByBranchId(@Param("branchId") Long branchId);
}


