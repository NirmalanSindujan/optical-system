package com.optical.modules.product.repository;

import com.optical.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductIdAndDeletedAtIsNull(Long productId);
    boolean existsBySkuAndDeletedAtIsNull(String sku);
    boolean existsByBarcodeAndDeletedAtIsNull(String barcode);
}


