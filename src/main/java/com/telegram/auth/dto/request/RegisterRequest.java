package com.telegram.auth.dto.request;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String confirmPassword,
        String displayName
) {}
