package com.optical.modules.branch.service;

import com.optical.common.exception.DuplicateResourceException;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.branch.dto.BranchRequest;
import com.optical.modules.branch.dto.BranchResponse;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    public BranchResponse create(BranchRequest request) {

        if (branchRepository.findByCodeAndDeletedAtIsNull(request.getCode()).isPresent()) {
            throw new DuplicateResourceException("Branch code already exists");
        }

        Branch branch = new Branch();
        applyRequest(branch, request);

        Branch saved = branchRepository.save(branch);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAll() {

        return branchRepository.findAll()
                .stream()
                .filter(b -> b.getDeletedAt() == null)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getById(Long id) {
        Branch branch = branchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        return mapToResponse(branch);
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request) {
        Branch branch = branchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        if (branchRepository.existsByCodeAndDeletedAtIsNullAndIdNot(request.getCode(), id)) {
            throw new DuplicateResourceException("Branch code already exists");
        }

        applyRequest(branch, request);
        Branch saved = branchRepository.save(branch);
        return mapToResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Branch branch = branchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        branch.setDeletedAt(LocalDateTime.now());
        branchRepository.save(branch);
    }

    private void applyRequest(Branch branch, BranchRequest request) {
        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setIsMain(request.getIsMain() != null ? request.getIsMain() : false);
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
