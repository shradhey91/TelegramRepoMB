package com.telegram.auth.dto.request;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}