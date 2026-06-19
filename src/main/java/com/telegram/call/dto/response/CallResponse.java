package com.telegram.call.dto.response;

import com.telegram.common.enums.CallStatus;
import com.telegram.common.enums.CallType;

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