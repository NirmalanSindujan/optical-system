package com.optical.modules.auth.controller;

import com.optical.modules.auth.dto.LoginRequest;
import com.optical.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}