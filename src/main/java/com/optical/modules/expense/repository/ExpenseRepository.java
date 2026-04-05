package com.optical.modules.expense.repository;

import com.optical.modules.expense.entity.Expense;
import com.optical.modules.expense.enums.ExpenseSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    @EntityGraph(attributePaths = {"branch", "category"})
    Optional<Expense> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.deletedAt is null
              and e.source = :source
            """)
    BigDecimal sumAmountBySource(@Param("source") ExpenseSource source);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.deletedAt is null
              and e.branch.id = :branchId
              and e.source = :source
            """)
    BigDecimal sumAmountByBranchIdAndSource(@Param("branchId") Long branchId, @Param("source") ExpenseSource source);

    @EntityGraph(attributePaths = {"branch", "category"})
    @Query("""
            select e
            from Expense e
            where e.deletedAt is null
              and e.branch.id = :branchId
              and e.source = com.optical.modules.expense.enums.ExpenseSource.CASH
            order by e.expenseDate asc, e.createdAt asc, e.id asc
            """)
    List<Expense> findCashLedgerEntriesByBranchId(@Param("branchId") Long branchId);

    @EntityGraph(attributePaths = {"branch", "category"})
    @Query("""
            select e
            from Expense e
            where e.deletedAt is null
              and e.branch.id = :branchId
              and e.source = com.optical.modules.expense.enums.ExpenseSource.CASH
              and e.expenseDate >= :fromDate
            order by e.expenseDate asc, e.createdAt asc, e.id asc
            """)
    List<Expense> findCashLedgerEntriesByBranchIdAndExpenseDateGreaterThanEqual(
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDate fromDate
    );

    @EntityGraph(attributePaths = {"branch", "category"})
    @Query("""
            select e
            from Expense e
            where e.deletedAt is null
              and e.branch.id = :branchId
              and e.source = com.optical.modules.expense.enums.ExpenseSource.CASH
              and e.expenseDate <= :toDate
            order by e.expenseDate asc, e.createdAt asc, e.id asc
            """)
    List<Expense> findCashLedgerEntriesByBranchIdAndExpenseDateLessThanEqual(
            @Param("branchId") Long branchId,
            @Param("toDate") LocalDate toDate
    );

    @EntityGraph(attributePaths = {"branch", "category"})
    @Query("""
            select e
            from Expense e
            where e.deletedAt is null
              and e.branch.id = :branchId
              and e.source = com.optical.modules.expense.enums.ExpenseSource.CASH
              and e.expenseDate between :fromDate and :toDate
            order by e.expenseDate asc, e.createdAt asc, e.id asc
            """)
    List<Expense> findCashLedgerEntriesByBranchIdAndExpenseDateBetween(
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
