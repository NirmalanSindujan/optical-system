package com.optical.modules.expense.controller;

import com.optical.modules.expense.dto.ExpenseCategoryRequest;
import com.optical.modules.expense.dto.ExpenseCategoryResponse;
import com.optical.modules.expense.service.ExpenseCategoryService;
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
import java.util.List;

@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_USER')")
@Tag(name = "Expense Categories", description = "Expense category and recurring reminder APIs")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    @PostMapping
    public ExpenseCategoryResponse create(@Valid @RequestBody ExpenseCategoryRequest request) {
        return expenseCategoryService.create(request);
    }

    @GetMapping
    public List<ExpenseCategoryResponse> getAll() {
        return expenseCategoryService.getAll();
    }

    @GetMapping("/recurring")
    public List<ExpenseCategoryResponse> getRecurringDueOn(@RequestParam(required = false) LocalDate date) {
        return expenseCategoryService.getRecurringDueOn(date);
    }

    @PutMapping("/{id}")
    public ExpenseCategoryResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseCategoryRequest request) {
        return expenseCategoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        expenseCategoryService.delete(id);
    }
}
