package com.telegram.user.dto.request;

public record UpdateProfileRequest(
        String displayName,
        String bio,
        String avatarUrl
) {}