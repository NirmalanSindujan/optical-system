package com.optical.modules.supplier.repository;

import com.optical.modules.supplier.entity.SupplierPaymentAllocation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SupplierPaymentAllocationRepository extends JpaRepository<SupplierPaymentAllocation, Long> {

    @EntityGraph(attributePaths = {"supplierCreditLedger", "supplierCreditLedger.supplier", "stockPurchase", "stockPurchase.branch"})
    @Query("""
            select a
            from SupplierPaymentAllocation a
            join a.supplierCreditLedger l
            join a.stockPurchase p
            where a.deletedAt is null
              and l.deletedAt is null
              and p.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
              and p.branch.id = :branchId
            order by l.entryDate asc, l.createdAt asc, a.id asc
            """)
    List<SupplierPaymentAllocation> findCashLedgerEntriesByBranchId(@Param("branchId") Long branchId);

    @EntityGraph(attributePaths = {"supplierCreditLedger", "supplierCreditLedger.supplier", "stockPurchase", "stockPurchase.branch"})
    @Query("""
            select a
            from SupplierPaymentAllocation a
            join a.supplierCreditLedger l
            join a.stockPurchase p
            where a.deletedAt is null
              and l.deletedAt is null
              and p.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
              and p.branch.id = :branchId
              and l.entryDate >= :fromDate
            order by l.entryDate asc, l.createdAt asc, a.id asc
            """)
    List<SupplierPaymentAllocation> findCashLedgerEntriesByBranchIdAndEntryDateGreaterThanEqual(
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDate fromDate
    );

    @EntityGraph(attributePaths = {"supplierCreditLedger", "supplierCreditLedger.supplier", "stockPurchase", "stockPurchase.branch"})
    @Query("""
            select a
            from SupplierPaymentAllocation a
            join a.supplierCreditLedger l
            join a.stockPurchase p
            where a.deletedAt is null
              and l.deletedAt is null
              and p.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
              and p.branch.id = :branchId
              and l.entryDate <= :toDate
            order by l.entryDate asc, l.createdAt asc, a.id asc
            """)
    List<SupplierPaymentAllocation> findCashLedgerEntriesByBranchIdAndEntryDateLessThanEqual(
            @Param("branchId") Long branchId,
            @Param("toDate") LocalDate toDate
    );

    @EntityGraph(attributePaths = {"supplierCreditLedger", "supplierCreditLedger.supplier", "stockPurchase", "stockPurchase.branch"})
    @Query("""
            select a
            from SupplierPaymentAllocation a
            join a.supplierCreditLedger l
            join a.stockPurchase p
            where a.deletedAt is null
              and l.deletedAt is null
              and p.deletedAt is null
              and l.entryType = com.optical.modules.supplier.entity.SupplierCreditEntryType.PAYMENT
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CASH
              and p.branch.id = :branchId
              and l.entryDate between :fromDate and :toDate
            order by l.entryDate asc, l.createdAt asc, a.id asc
            """)
    List<SupplierPaymentAllocation> findCashLedgerEntriesByBranchIdAndEntryDateBetween(
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
