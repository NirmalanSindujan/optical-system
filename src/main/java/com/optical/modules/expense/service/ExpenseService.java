package com.optical.modules.expense.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.expense.dto.ExpensePageResponse;
import com.optical.modules.expense.dto.ExpenseRequest;
import com.optical.modules.expense.dto.ExpenseResponse;
import com.optical.modules.expense.entity.Expense;
import com.optical.modules.expense.entity.ExpenseCategory;
import com.optical.modules.expense.repository.ExpenseCategoryRepository;
import com.optical.modules.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense();
        applyRequest(expense, request);
        return mapToResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return mapToResponse(expense);
    }

    @Transactional(readOnly = true)
    public ExpensePageResponse search(
            String q,
            Long branchId,
            com.optical.modules.expense.enums.ExpenseSource source,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        Page<Expense> result = expenseRepository.findAll(
                buildSpecification(normalize(q), branchId, source, fromDate, toDate),
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")))
        );

        return ExpensePageResponse.builder()
                .items(result.getContent().stream().map(this::mapToResponse).toList())
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    private Specification<Expense> buildSpecification(
            String q,
            Long branchId,
            com.optical.modules.expense.enums.ExpenseSource source,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            if (branchId != null) {
                predicates.add(criteriaBuilder.equal(root.get("branch").get("id"), branchId));
            }
            if (source != null) {
                predicates.add(criteriaBuilder.equal(root.get("source"), source));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("expenseDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("expenseDate"), toDate));
            }
            if (q != null) {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("reference")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("category").get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("branch").get("name")), pattern)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        applyRequest(expense, request);
        return mapToResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expense.setDeletedAt(LocalDateTime.now());
        expenseRepository.save(expense);
    }

    private void applyRequest(Expense expense, ExpenseRequest request) {
        Branch branch = branchRepository.findByIdAndDeletedAtIsNull(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        ExpenseCategory category = expenseCategoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found"));

        expense.setBranch(branch);
        expense.setCategory(category);
        expense.setAmount(scale(request.getAmount()));
        expense.setDescription(normalize(request.getDescription()));
        expense.setSource(request.getSource());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setReference(normalize(request.getReference()));
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .branchId(expense.getBranch().getId())
                .branchName(expense.getBranch().getName())
                .categoryId(expense.getCategory().getId())
                .categoryName(expense.getCategory().getName())
                .categoryRecurringType(expense.getCategory().getRecurringType())
                .categoryReminderDate(expense.getCategory().getReminderDate())
                .amount(expense.getAmount())
                .description(expense.getDescription())
                .source(expense.getSource())
                .expenseDate(expense.getExpenseDate())
                .reference(expense.getReference())
                .createdAt(expense.getCreatedAt())
                .build();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
