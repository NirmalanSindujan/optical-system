package com.optical.modules.product.repository;

import com.optical.modules.supplier.dto.SupplierProductStockResponse;
import com.optical.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductIdAndDeletedAtIsNull(Long productId);
    boolean existsBySkuAndDeletedAtIsNull(String sku);
    boolean existsByBarcodeAndDeletedAtIsNull(String barcode);

    @Query("""
            select new com.optical.modules.supplier.dto.SupplierProductStockResponse(
                p.id,
                pv.id,
                p.name,
                pv.sku,
                pv.sellingPrice,
                pv.quantity
            )
            from SupplierProduct sp
            join sp.product p
            join ProductVariant pv on pv.product.id = p.id
            where sp.supplierId = :supplierId
              and sp.deletedAt is null
              and p.deletedAt is null
              and pv.deletedAt is null
            order by p.name asc, pv.id asc
            """)
    List<SupplierProductStockResponse> findStockBySupplierId(@Param("supplierId") Long supplierId);
}


