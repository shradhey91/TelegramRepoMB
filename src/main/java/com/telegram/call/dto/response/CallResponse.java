package com.telegram.call.dto.response;

import com.telegram.common.enums.CallStatus;
import com.telegram.common.enums.CallType;

import java.time.OffsetDateTime;
import java.util.List;

public record CallResponse(
        Long callId,
        Long creatorId,
        String creatorName,
        String creatorAvatarUrl,
        CallType callType,
        CallStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Long durationSeconds,
        List<ParticipantResponse> participants
) {}