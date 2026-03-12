package com.optical.modules.purchase.repository;

import com.optical.modules.purchase.entity.StockPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockPurchaseRepository extends JpaRepository<StockPurchase, Long> {

    @Query(
            value = """
                    select sp.id
                    from StockPurchase sp
                    where sp.deletedAt is null
                    order by sp.purchaseDate desc, sp.id desc
                    """,
            countQuery = """
                    select count(sp.id)
                    from StockPurchase sp
                    where sp.deletedAt is null
                    """
    )
    Page<Long> findActiveIds(Pageable pageable);

    @Query(
            value = """
                    select sp.id
                    from StockPurchase sp
                    where sp.deletedAt is null
                      and sp.id = :id
                    """,
            countQuery = """
                    select count(sp.id)
                    from StockPurchase sp
                    where sp.deletedAt is null
                      and sp.id = :id
                    """
    )
    Page<Long> findActiveIdsById(@Param("id") Long id, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "branch", "items", "items.variant", "items.variant.product"})
    @Query("select distinct sp from StockPurchase sp where sp.id in :ids")
    List<StockPurchase> findDetailedByIdIn(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"supplier", "branch", "items", "items.variant", "items.variant.product"})
    Optional<StockPurchase> findByIdAndDeletedAtIsNull(Long id);
}
