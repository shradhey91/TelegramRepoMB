package com.telegram.auth.dto.request;

public record LoginRequest(
        String email,
        String password
) {}