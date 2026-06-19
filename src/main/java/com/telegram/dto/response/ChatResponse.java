package com.telegram.dto.response;

import com.telegram.enums.ChatType;

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