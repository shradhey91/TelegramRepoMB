package com.telegram.stories.dto.response;

import java.util.List;

public record StoryGroupResponse(
        Long userId,
        String username,
        String avatarUrl,
        boolean hasUnseenStories,
        List<StoryResponse> stories
) {}
