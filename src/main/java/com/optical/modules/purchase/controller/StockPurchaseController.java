package com.optical.modules.purchase.controller;

import com.optical.modules.purchase.dto.StockPurchaseCreateRequest;
import com.optical.modules.purchase.dto.StockPurchaseResponse;
import com.optical.modules.purchase.service.StockPurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-purchases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class StockPurchaseController {

    private final StockPurchaseService stockPurchaseService;

    @PostMapping
    public StockPurchaseResponse create(@Valid @RequestBody StockPurchaseCreateRequest request) {
        return stockPurchaseService.create(request);
    }

    @GetMapping("/{id}")
    public StockPurchaseResponse getById(@PathVariable Long id) {
        return stockPurchaseService.getById(id);
    }
}
