package com.telegram.call.dto.request;

import com.telegram.common.enums.CallType;

import java.util.List;

public record InitiateCallRequest(
        List<Long> participantIds,
        CallType callType
) {}