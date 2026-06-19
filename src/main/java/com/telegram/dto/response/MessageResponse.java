package com.telegram.dto.response;

import com.telegram.enums.MessageType;

import java.time.LocalDateTime;

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
        LocalDateTime createdAt,
        LocalDateTime editedAt
) {}