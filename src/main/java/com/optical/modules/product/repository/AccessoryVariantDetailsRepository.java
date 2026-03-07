package com.optical.modules.product.repository;

import com.optical.modules.product.entity.AccessoryVariantDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccessoryVariantDetailsRepository extends JpaRepository<AccessoryVariantDetails, Long> {

    @Query("""
            select a from AccessoryVariantDetails a
            join a.variant v
            join v.product p
            where p.deletedAt is null
              and v.deletedAt is null
              and (
                   :q is null or :q = ''
                   or lower(coalesce(p.name, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.brandName, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.sku, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.barcode, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<AccessoryVariantDetails> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select a from AccessoryVariantDetails a
            join a.variant v
            join v.product p
            where p.id = :productId
              and p.deletedAt is null
              and v.deletedAt is null
            """)
    Optional<AccessoryVariantDetails> findByProductId(@Param("productId") Long productId);
}


