package com.telegram.stories.dto.response;

import java.time.OffsetDateTime;

public record StoryViewerResponse(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        OffsetDateTime viewedAt
) {}
