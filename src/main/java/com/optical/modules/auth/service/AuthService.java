package com.optical.modules.auth.service;

import com.optical.modules.auth.dto.LoginRequest;
import com.optical.modules.auth.dto.LoginResponse;
import com.optical.modules.users.entity.User;
import com.optical.modules.users.repository.UserRepository;
import com.optical.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsernameAndDeletedAtIsNull(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid username or password");
        }

        String role = user.getRole().name();
        Long branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        String token = jwtUtil.generateToken(
                user.getUsername(),
                role,
                branchId
        );

        return new LoginResponse(token, role, branchId, user.getUsername());
    }
}
