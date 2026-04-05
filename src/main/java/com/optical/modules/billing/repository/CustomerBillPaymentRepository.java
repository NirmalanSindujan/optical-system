package com.optical.modules.billing.repository;

import com.optical.common.enums.ChequeStatus;
import com.optical.modules.billing.entity.CustomerBillPayment;
import com.optical.modules.purchase.entity.PaymentMode;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface CustomerBillPaymentRepository extends JpaRepository<CustomerBillPayment, Long> {

    List<CustomerBillPayment> findByCustomerBillIdOrderByIdAsc(Long customerBillId);

    @EntityGraph(attributePaths = {"customerBill", "customerBill.customer"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from CustomerBillPayment p
            join p.customerBill b
            left join b.customer c
            where p.id = :id
              and p.deletedAt is null
              and b.deletedAt is null
            """)
    Optional<CustomerBillPayment> findActiveByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customerBill", "customerBill.customer"})
    @Query("""
            select p
            from CustomerBillPayment p
            join p.customerBill b
            left join b.customer c
            where p.deletedAt is null
              and b.deletedAt is null
              and p.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CHEQUE
              and (:customerId is null or c.id = :customerId)
              and (:status is null or p.chequeStatus = :status)
            order by coalesce(p.chequeDate, b.billDate) desc, p.id desc
            """)
    Page<CustomerBillPayment> findReceivedCheques(
            @Param("customerId") Long customerId,
            @Param("status") ChequeStatus status,
            Pageable pageable
    );

    @Query("""
            select coalesce(sum(p.amount), 0)
            from CustomerBillPayment p
            join p.customerBill b
            where p.deletedAt is null
              and b.deletedAt is null
              and (
                  p.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
                  or (
                      p.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CHEQUE
                      and p.chequeStatus = com.optical.common.enums.ChequeStatus.CLEARED
                      and p.chequeSettlementMode = com.optical.modules.purchase.entity.PaymentMode.CASH
                  )
              )
            """)
    BigDecimal sumCashCollections();

    @Query("""
            select coalesce(sum(p.amount), 0)
            from CustomerBillPayment p
            join p.customerBill b
            where p.deletedAt is null
              and b.deletedAt is null
              and (
                  p.paymentMode = com.optical.modules.purchase.entity.PaymentMode.BANK
                  or (
                      p.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CHEQUE
                      and p.chequeStatus = com.optical.common.enums.ChequeStatus.CLEARED
                      and p.chequeSettlementMode = com.optical.modules.purchase.entity.PaymentMode.BANK
                  )
              )
            """)
    BigDecimal sumBankCollections();

    @Query("""
            select coalesce(sum(p.amount), 0)
            from CustomerBillPayment p
            join p.customerBill b
            where p.deletedAt is null
              and b.deletedAt is null
              and b.branch.id = :branchId
              and (
                  p.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
                  or (
                      p.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CHEQUE
                      and p.chequeStatus = com.optical.common.enums.ChequeStatus.CLEARED
                      and p.chequeSettlementMode = com.optical.modules.purchase.entity.PaymentMode.CASH
                  )
              )
            """)
    BigDecimal sumCashCollectionsByBranchId(@Param("branchId") Long branchId);
}
