package com.telegram.chat.dto.response;

import com.telegram.common.enums.MemberRole;

import java.time.LocalDateTime;

public record ChatMemberResponse(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        MemberRole role,
        Boolean isOnline,
        LocalDateTime joinedAt
) {}