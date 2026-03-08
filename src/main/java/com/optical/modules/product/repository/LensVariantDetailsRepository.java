package com.optical.modules.product.repository;

import com.optical.modules.product.entity.LensVariantDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LensVariantDetailsRepository extends JpaRepository<LensVariantDetails, Long> {

    @Query("""
            select l from LensVariantDetails l
            join l.variant v
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
    Page<LensVariantDetails> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select l from LensVariantDetails l
            join l.variant v
            join v.product p
            where p.deletedAt is null
              and v.deletedAt is null
              and l.lensSubType = :lensSubType
              and (
                   :q is null or :q = ''
                   or lower(coalesce(p.name, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.brandName, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.sku, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.barcode, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<LensVariantDetails> searchByLensSubType(
            @Param("lensSubType") String lensSubType,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            select l.lensSubType as lensSubType,
                   count(l) as totalCounts
            from LensVariantDetails l
            join l.variant v
            join v.product p
            where p.deletedAt is null
              and v.deletedAt is null
              and l.lensSubType is not null
            group by l.lensSubType
            order by l.lensSubType
            """)
    List<LensSubtabProjection> findLensSubtabs();

    @Query("""
            select l from LensVariantDetails l
            join l.variant v
            join v.product p
            where v.id = :variantId
              and p.deletedAt is null
              and v.deletedAt is null
            """)
    Optional<LensVariantDetails> findByVariantId(@Param("variantId") Long variantId);

    @Query("""
            select l from LensVariantDetails l
            join l.variant v
            join v.product p
            where p.id = :productId
              and p.deletedAt is null
              and v.deletedAt is null
            """)
    Optional<LensVariantDetails> findByProductId(@Param("productId") Long productId);
}


