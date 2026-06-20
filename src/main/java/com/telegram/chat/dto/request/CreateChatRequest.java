package com.telegram.chat.dto.request;

import com.telegram.common.enums.ChatType;

import java.util.List;

public record CreateChatRequest(
        ChatType type,
        String title,
        String description,
        List<Long> memberIds
) {}