package com.optical.modules.supplier.repository;

import com.optical.common.enums.ChequeStatus;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.supplier.entity.SupplierCreditLedger;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierCreditLedgerRepository extends JpaRepository<SupplierCreditLedger, Long> {

    @EntityGraph(attributePaths = {"stockPurchase", "branch", "allocations", "allocations.stockPurchase"})
    List<SupplierCreditLedger> findBySupplierIdOrderByEntryDateDescIdDesc(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "branch", "allocations", "allocations.stockPurchase"})
    @Query("""
            select l
            from SupplierCreditLedger l
            where l.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CHEQUE
              and (:supplierId is null or l.supplier.id = :supplierId)
              and (:status is null or l.chequeStatus = :status)
            order by coalesce(l.chequeDate, l.entryDate) desc, l.id desc
            """)
    Page<SupplierCreditLedger> findProvidedCheques(
            @Param("supplierId") Long supplierId,
            @Param("status") ChequeStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"supplier", "allocations", "allocations.stockPurchase"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l
            from SupplierCreditLedger l
            where l.id = :id
              and l.deletedAt is null
            """)
    Optional<SupplierCreditLedger> findActiveByIdForUpdate(@Param("id") Long id);

    @Query("""
            select coalesce(sum(-l.amount), 0)
            from SupplierCreditLedger l
            where l.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = :paymentMode
            """)
    BigDecimal sumPaymentOutflowByPaymentMode(@Param("paymentMode") PaymentMode paymentMode);

    @Query("""
            select coalesce(sum(-l.amount), 0)
            from SupplierCreditLedger l
            where l.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.branch.id = :branchId
              and l.paymentMode = :paymentMode
            """)
    BigDecimal sumPaymentOutflowByBranchIdAndPaymentMode(
            @Param("branchId") Long branchId,
            @Param("paymentMode") PaymentMode paymentMode
    );

    @Query("""
            select coalesce(sum(-l.amount), 0)
            from SupplierCreditLedger l
            where l.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CHEQUE
              and l.chequeStatus = :status
            """)
    BigDecimal sumChequePaymentOutflowByStatus(@Param("status") ChequeStatus status);

    @EntityGraph(attributePaths = {"supplier", "branch"})
    @Query("""
            select l
            from SupplierCreditLedger l
            where l.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
              and l.branch.id = :branchId
            order by l.entryDate asc, l.createdAt asc, l.id asc
            """)
    List<SupplierCreditLedger> findCashLedgerEntriesByBranchId(@Param("branchId") Long branchId);

    @EntityGraph(attributePaths = {"supplier", "branch"})
    @Query("""
            select l
            from SupplierCreditLedger l
            where l.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
              and l.branch.id = :branchId
              and l.entryDate >= :fromDate
            order by l.entryDate asc, l.createdAt asc, l.id asc
            """)
    List<SupplierCreditLedger> findCashLedgerEntriesByBranchIdAndEntryDateGreaterThanEqual(
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDate fromDate
    );

    @EntityGraph(attributePaths = {"supplier", "branch"})
    @Query("""
            select l
            from SupplierCreditLedger l
            where l.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
              and l.branch.id = :branchId
              and l.entryDate <= :toDate
            order by l.entryDate asc, l.createdAt asc, l.id asc
            """)
    List<SupplierCreditLedger> findCashLedgerEntriesByBranchIdAndEntryDateLessThanEqual(
            @Param("branchId") Long branchId,
            @Param("toDate") LocalDate toDate
    );

    @EntityGraph(attributePaths = {"supplier", "branch"})
    @Query("""
            select l
            from SupplierCreditLedger l
            where l.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
              and l.branch.id = :branchId
              and l.entryDate between :fromDate and :toDate
            order by l.entryDate asc, l.createdAt asc, l.id asc
            """)
    List<SupplierCreditLedger> findCashLedgerEntriesByBranchIdAndEntryDateBetween(
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
