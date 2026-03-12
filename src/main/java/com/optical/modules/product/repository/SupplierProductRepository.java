package com.optical.modules.product.repository;

import com.optical.modules.product.entity.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    boolean existsBySupplierIdAndProductIdAndDeletedAtIsNull(Long supplierId, Long productId);

    @Query("""
            select sp.supplierId from SupplierProduct sp
            where sp.product.id = :productId
              and sp.deletedAt is null
            order by sp.id
            """)
    List<Long> findActiveSupplierIdsByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("""
            update SupplierProduct sp
            set sp.deletedAt = :deletedAt
            where sp.product.id = :productId
              and sp.deletedAt is null
            """)
    int softDeleteActiveByProductId(@Param("productId") Long productId, @Param("deletedAt") LocalDateTime deletedAt);
}
