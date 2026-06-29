package com.telegram.user.dto.response;

import java.time.OffsetDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        Boolean isOnline,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt
) {}
