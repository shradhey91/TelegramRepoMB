package com.telegram.stories.dto.request;

import com.telegram.stories.entities.StoryType;

public record CreateStoryRequest(
        String mediaUrl,
        String caption,
        StoryType type
) {}
