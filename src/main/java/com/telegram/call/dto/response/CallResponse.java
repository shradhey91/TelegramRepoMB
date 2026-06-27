package com.telegram.call.dto.response;

import com.telegram.common.enums.CallStatus;
import com.telegram.common.enums.CallType;

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
        java.time.OffsetDateTime createdAt,
        java.time.OffsetDateTime startedAt,
        java.time.OffsetDateTime endedAt,
        Long durationSeconds
) {}