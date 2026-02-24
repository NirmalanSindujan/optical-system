package com.optical.modules.branch.controller;

import com.optical.modules.branch.dto.BranchRequest;
import com.optical.modules.branch.dto.BranchResponse;
import com.optical.modules.branch.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BranchResponse create(@Valid @RequestBody BranchRequest request) {
        return branchService.create(request);
    }

    @GetMapping
    public List<BranchResponse> getAll() {
        return branchService.getAll();
    }
}