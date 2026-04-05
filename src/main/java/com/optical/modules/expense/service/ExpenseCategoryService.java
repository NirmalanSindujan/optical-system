package com.optical.modules.expense.service;

import com.optical.common.exception.DuplicateResourceException;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.expense.dto.ExpenseCategoryRequest;
import com.optical.modules.expense.dto.ExpenseCategoryResponse;
import com.optical.modules.expense.entity.ExpenseCategory;
import com.optical.modules.expense.enums.RecurringType;
import com.optical.modules.expense.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.optical.common.util.StringNormalizer.normalize;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Transactional
    public ExpenseCategoryResponse create(ExpenseCategoryRequest request) {
        String name = normalize(request.getName());
        if (expenseCategoryRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
            throw new DuplicateResourceException("Expense category already exists");
        }

        ExpenseCategory category = new ExpenseCategory();
        applyRequest(category, request);
        return mapToResponse(expenseCategoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> getAll() {
        return expenseCategoryRepository.findAllByDeletedAtIsNullOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> getRecurringDueOn(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return expenseCategoryRepository
                .findByDeletedAtIsNullAndRecurringTypeNotAndReminderDateOrderByNameAsc(RecurringType.NONE, targetDate)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ExpenseCategoryResponse update(Long id, ExpenseCategoryRequest request) {
        ExpenseCategory category = expenseCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found"));

        String name = normalize(request.getName());
        if (expenseCategoryRepository.existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(name, id)) {
            throw new DuplicateResourceException("Expense category already exists");
        }

        applyRequest(category, request);
        return mapToResponse(expenseCategoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        ExpenseCategory category = expenseCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found"));
        category.setDeletedAt(LocalDateTime.now());
        expenseCategoryRepository.save(category);
    }

    private void applyRequest(ExpenseCategory category, ExpenseCategoryRequest request) {
        RecurringType recurringType = request.getRecurringType();
        if (recurringType == RecurringType.NONE) {
            category.setReminderDate(null);
        } else {
            if (request.getReminderDate() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "reminderDate is required for recurring categories");
            }
            category.setReminderDate(request.getReminderDate());
        }

        category.setName(normalize(request.getName()));
        category.setDescription(normalize(request.getDescription()));
        category.setRecurringType(recurringType);
    }

    private ExpenseCategoryResponse mapToResponse(ExpenseCategory category) {
        return ExpenseCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .recurringType(category.getRecurringType())
                .reminderDate(category.getReminderDate())
                .build();
    }
}
