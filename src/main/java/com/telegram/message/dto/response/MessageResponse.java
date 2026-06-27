package com.telegram.message.dto.response;

import com.telegram.common.enums.MessageType;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long id,
        Long chatId,
        Long senderId,
        String senderName,
        String senderAvatarUrl,
        MessageType type,
        String content,
        Long replyToId,
        String replyToContent,
        Boolean isEdited,
        OffsetDateTime createdAt,
        OffsetDateTime editedAt
) {}