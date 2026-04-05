package com.optical.modules.finance.controller;

import com.optical.modules.finance.dto.BusinessSummaryResponse;
import com.optical.modules.finance.service.BusinessSummaryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Finance", description = "Business finance summary APIs")
@SecurityRequirement(name = "bearerAuth")
public class BusinessSummaryController {

    private final BusinessSummaryService businessSummaryService;

    @GetMapping("/business-summary")
    public BusinessSummaryResponse getSummary() {
        return businessSummaryService.getSummary();
    }
}
