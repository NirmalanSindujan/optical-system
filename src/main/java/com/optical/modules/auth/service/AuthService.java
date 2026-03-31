package com.optical.modules.auth.service;

import com.optical.modules.auth.dto.LoginRequest;
import com.optical.modules.auth.dto.LoginResponse;
import com.optical.modules.users.entity.User;
import com.optical.modules.users.repository.UserRepository;
import com.optical.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsernameAndDeletedAtIsNull(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
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
