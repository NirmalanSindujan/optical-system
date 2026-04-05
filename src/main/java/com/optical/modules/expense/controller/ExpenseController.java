package com.optical.modules.expense.controller;

import com.optical.modules.expense.dto.ExpensePageResponse;
import com.optical.modules.expense.dto.ExpenseRequest;
import com.optical.modules.expense.dto.ExpenseResponse;
import com.optical.modules.expense.enums.ExpenseSource;
import com.optical.modules.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_USER')")
@Tag(name = "Expenses", description = "Expense management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(request);
    }

    @GetMapping("/{id}")
    public ExpenseResponse getById(@PathVariable Long id) {
        return expenseService.getById(id);
    }

    @GetMapping
    public ExpensePageResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) ExpenseSource source,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return expenseService.search(q, branchId, source, fromDate, toDate, page, size);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }
}
