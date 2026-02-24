package com.optical.modules.branch.service;

import com.optical.modules.branch.dto.BranchRequest;
import com.optical.modules.branch.dto.BranchResponse;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchResponse create(BranchRequest request) {

        if (branchRepository.findByCodeAndDeletedAtIsNull(request.getCode()).isPresent()) {
            throw new RuntimeException("Branch code already exists");
        }

        Branch branch = new Branch();
        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setIsMain(
                request.getIsMain() != null ? request.getIsMain() : false
        );

        Branch saved = branchRepository.save(branch);

        return mapToResponse(saved);
    }

    public List<BranchResponse> getAll() {

        return branchRepository.findAll()
                .stream()
                .filter(b -> b.getDeletedAt() == null)
                .map(this::mapToResponse)
                .toList();
    }

    private BranchResponse mapToResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .isMain(branch.getIsMain())
                .build();
    }
}