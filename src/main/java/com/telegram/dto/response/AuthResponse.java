package com.telegram.dto.response;

public record AuthResponse(
        String accessToken,
        UserProfileResponse user
) {}
