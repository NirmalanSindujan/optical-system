package com.optical.modules.billing.repository;

import com.optical.modules.billing.entity.CustomerBill;
import com.optical.modules.purchase.entity.PaymentMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerBillRepository extends JpaRepository<CustomerBill, Long> {

    @EntityGraph(attributePaths = {"customer", "patient", "branch", "prescription"})
    Optional<CustomerBill> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select b.id
            from CustomerBill b
            where b.deletedAt is null
              and (:branchId is null or b.branch.id = :branchId)
            order by b.billDate desc, b.id desc
            """)
    Page<Long> findActiveIdsByBranchId(@Param("branchId") Long branchId, Pageable pageable);

    @Query("""
            select b.id
            from CustomerBill b
            left join b.customer c
            left join b.patient p
            where b.deletedAt is null
              and (:branchId is null or b.branch.id = :branchId)
              and (
                    :keyword is null
                    or lower(cast(coalesce(b.billNumber, '') as string)) like lower(concat('%', :keyword, '%'))
                    or lower(cast(coalesce(c.name, '') as string)) like lower(concat('%', :keyword, '%'))
                    or lower(cast(coalesce(p.name, '') as string)) like lower(concat('%', :keyword, '%'))
                  )
            order by b.billDate desc, b.id desc
            """)
    Page<Long> findActiveIds(@Param("keyword") String keyword, @Param("branchId") Long branchId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "patient", "branch", "prescription"})
    List<CustomerBill> findByIdIn(List<Long> ids);

    @Query("""
            select count(b)
            from CustomerBill b
            where b.deletedAt is null
              and b.customer.id = :customerId
            """)
    long countActiveByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            select coalesce(sum(b.totalAmount), 0)
            from CustomerBill b
            where b.deletedAt is null
              and b.customer.id = :customerId
            """)
    BigDecimal sumTotalAmountByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            select coalesce(sum(b.paidAmount), 0)
            from CustomerBill b
            where b.deletedAt is null
              and b.customer.id = :customerId
            """)
    BigDecimal sumPaidAmountByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            select coalesce(sum(b.balanceAmount), 0)
            from CustomerBill b
            where b.deletedAt is null
              and b.customer.id = :customerId
            """)
    BigDecimal sumBalanceAmountByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            select coalesce(sum(b.totalAmount), 0)
            from CustomerBill b
            where b.deletedAt is null
              and b.branch.id = :branchId
            """)
    BigDecimal sumTotalAmountByBranchId(@Param("branchId") Long branchId);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from CustomerBillPayment p
            join p.customerBill b
            where p.deletedAt is null
              and b.deletedAt is null
              and b.branch.id = :branchId
              and p.paymentMode = :paymentMode
            """)
    BigDecimal sumTotalAmountByBranchIdAndPaymentMode(@Param("branchId") Long branchId,
                                                      @Param("paymentMode") PaymentMode paymentMode);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from CustomerBillPayment p
            join p.customerBill b
            where p.deletedAt is null
              and b.deletedAt is null
              and p.paymentMode = :paymentMode
            """)
    BigDecimal sumTotalAmountByPaymentMode(@Param("paymentMode") PaymentMode paymentMode);

    @Query("""
            select coalesce(sum(b.balanceAmount), 0)
            from CustomerBill b
            where b.deletedAt is null
              and b.branch.id = :branchId
            """)
    BigDecimal sumBalanceAmountByBranchId(@Param("branchId") Long branchId);
}
