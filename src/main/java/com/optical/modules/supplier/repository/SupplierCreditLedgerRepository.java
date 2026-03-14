package com.optical.modules.supplier.repository;

import com.optical.modules.supplier.entity.SupplierCreditLedger;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierCreditLedgerRepository extends JpaRepository<SupplierCreditLedger, Long> {

    @EntityGraph(attributePaths = {"stockPurchase", "allocations", "allocations.stockPurchase"})
    List<SupplierCreditLedger> findBySupplierIdOrderByEntryDateDescIdDesc(Long supplierId);
}
