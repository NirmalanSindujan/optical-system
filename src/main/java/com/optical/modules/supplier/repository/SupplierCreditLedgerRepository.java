package com.optical.modules.supplier.repository;

import com.optical.common.enums.ChequeStatus;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.supplier.entity.SupplierCreditLedger;
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

public interface SupplierCreditLedgerRepository extends JpaRepository<SupplierCreditLedger, Long> {

    @EntityGraph(attributePaths = {"stockPurchase", "allocations", "allocations.stockPurchase"})
    List<SupplierCreditLedger> findBySupplierIdOrderByEntryDateDescIdDesc(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "allocations", "allocations.stockPurchase"})
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
              and l.paymentMode = com.optical.modules.purchase.entity.PaymentMode.CHEQUE
              and l.chequeStatus = :status
            """)
    BigDecimal sumChequePaymentOutflowByStatus(@Param("status") ChequeStatus status);
}
