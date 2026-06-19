package com.telegram.dto.response;

import com.telegram.enums.CallStatus;
import com.telegram.enums.CallType;

import java.time.LocalDateTime;

public record CallResponse(
        Long callId,
        Long callerId,
        String callerName,
        String callerAvatarUrl,
        Long receiverId,
        String receiverName,
        String receiverAvatarUrl,
        CallType callType,
        CallStatus status,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long durationSeconds
) {}