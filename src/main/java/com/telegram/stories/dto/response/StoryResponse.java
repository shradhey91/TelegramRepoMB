package com.telegram.stories.dto.response;

import com.telegram.stories.entities.StoryType;

import java.time.OffsetDateTime;

public record StoryResponse(
        Long id,
        Long userId,
        String username,
        String avatarUrl,
        String mediaUrl,
        String caption,
        StoryType type,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        long viewerCount,
        boolean viewed
) {}
