package com.telegram.stories.dto.response;

import java.time.LocalDateTime;

public record StoryViewerResponse(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        LocalDateTime viewedAt
) {}
