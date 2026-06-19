package com.telegram.dto.response;

import com.telegram.enums.MemberRole;

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