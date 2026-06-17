package com.telegram.dto.response;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        Boolean isOnline,
        LocalDateTime lastSeenAt,
        LocalDateTime createdAt
) {}
