package com.telegram.dto.request;

import com.telegram.enums.MessageType;

public record SendMessageRequest(
        Long chatId,
        String content,
        MessageType type,
        Long replyToId
) {}