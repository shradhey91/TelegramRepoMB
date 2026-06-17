package com.telegram.dto.request;

public record ChangePasswordRequest(

        String currentPassword,
        String newPassword
) {}