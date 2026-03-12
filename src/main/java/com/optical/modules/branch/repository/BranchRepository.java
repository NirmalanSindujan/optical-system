package com.optical.modules.branch.repository;

import com.optical.modules.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByCodeAndDeletedAtIsNull(String code);
    Optional<Branch> findByIdAndDeletedAtIsNull(Long id);
    Optional<Branch> findFirstByIsMainTrueAndDeletedAtIsNull();

}
