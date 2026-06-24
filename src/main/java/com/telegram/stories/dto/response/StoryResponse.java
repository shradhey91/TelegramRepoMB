package com.telegram.stories.dto.response;

import com.telegram.stories.entities.StoryType;

import java.time.LocalDateTime;

public record StoryResponse(
        Long id,
        Long userId,
        String username,
        String avatarUrl,
        String mediaUrl,
        String caption,
        StoryType type,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        long viewerCount,
        boolean viewed
) {}
