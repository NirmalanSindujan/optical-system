package com.optical.modules.product.repository;

import com.optical.modules.product.entity.SunglassesVariantDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SunglassesVariantDetailsRepository extends JpaRepository<SunglassesVariantDetails, Long> {

    @Query("""
            select s from SunglassesVariantDetails s
            join s.variant v
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
    Page<SunglassesVariantDetails> search(@Param("q") String q, Pageable pageable);
}


