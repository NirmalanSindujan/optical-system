package com.optical.config;

public final class SecurityEndpoints {

    private SecurityEndpoints() {
    }

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
}
