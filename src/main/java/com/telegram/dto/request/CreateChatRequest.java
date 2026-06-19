package com.telegram.dto.request;

import com.telegram.enums.ChatType;

import java.util.List;

public record CreateChatRequest(
        ChatType type,
        String title,
        String description,
        List<Long> memberIds
) {}