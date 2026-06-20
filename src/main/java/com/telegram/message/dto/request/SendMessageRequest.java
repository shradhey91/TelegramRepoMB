package com.telegram.message.dto.request;

import com.telegram.common.enums.MessageType;

public record SendMessageRequest(
        Long chatId,
        String content,
        MessageType type,
        Long replyToId
) {}