package com.optical.modules.expense.repository;

import com.optical.modules.expense.entity.ExpenseCategory;
import com.optical.modules.expense.enums.RecurringType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    Optional<ExpenseCategory> findByIdAndDeletedAtIsNull(Long id);

    List<ExpenseCategory> findAllByDeletedAtIsNullOrderByNameAsc();

    List<ExpenseCategory> findByDeletedAtIsNullAndRecurringTypeNotAndReminderDateOrderByNameAsc(
            RecurringType recurringType,
            LocalDate reminderDate
    );

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, Long id);
}
