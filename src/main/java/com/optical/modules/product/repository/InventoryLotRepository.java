package com.optical.modules.product.repository;

import com.optical.modules.product.entity.InventoryLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryLotRepository extends JpaRepository<InventoryLot, Long> {
    @Query("""
            select lot
            from InventoryLot lot
            where lot.variant.id = :variantId
              and lot.deletedAt is null
              and lot.qtyRemaining > 0
            order by lot.purchasedAt asc, lot.id asc
            """)
    List<InventoryLot> findAvailableLotsByVariantIdOrderByPurchasedAtAsc(Long variantId);
}


