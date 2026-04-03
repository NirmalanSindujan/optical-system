package com.optical.modules.users.service;

import com.optical.common.exception.DuplicateResourceException;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.users.dto.UserCreateRequest;
import com.optical.modules.users.dto.UserResponse;
import com.optical.modules.users.dto.UserUpdateRequest;
import com.optical.modules.users.entity.Role;
import com.optical.modules.users.entity.User;
import com.optical.modules.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (userRepository.existsByUsernameAndDeletedAtIsNull(username)) {
            throw new DuplicateResourceException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        user.setRole(request.getRole());
        user.setBranch(resolveBranch(request.getRole(), request.getBranchId()));

        return mapToResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAllByDeletedAtIsNullOrderByIdDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String username = normalizeUsername(request.getUsername());
        if (userRepository.existsByUsernameAndDeletedAtIsNullAndIdNot(username, id)) {
            throw new DuplicateResourceException("Username already exists");
        }

        user.setUsername(username);
        user.setRole(request.getRole());
        user.setBranch(resolveBranch(request.getRole(), request.getBranchId()));

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private Branch resolveBranch(Role role, Long branchId) {
        if (role == Role.BRANCH_USER && branchId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branchId is required for BRANCH_USER");
        }

        if (branchId == null) {
            return null;
        }

        return branchRepository.findByIdAndDeletedAtIsNull(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    private UserResponse mapToResponse(User user) {
        Branch branch = user.getBranch();
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .branchId(branch != null ? branch.getId() : null)
                .branchCode(branch != null ? branch.getCode() : null)
                .branchName(branch != null ? branch.getName() : null)
                .build();
    }
}
