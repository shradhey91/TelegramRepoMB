package com.telegram.chat.dto.response;

import com.telegram.message.dto.response.MessageResponse;
import com.telegram.common.enums.ChatType;

import java.time.LocalDateTime;
import java.util.List;

public record ChatResponse(
        Long id,
        ChatType type,
        String title,
        String description,
        String avatarUrl,
        String inviteLink,
        Long createdById,
        LocalDateTime createdAt,
        List<ChatMemberResponse> members,
        MessageResponse lastMessage,
        long unreadCount
) {}