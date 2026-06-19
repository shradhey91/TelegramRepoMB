package com.telegram.call.dto.request;

import com.telegram.common.enums.CallType;

public record InitiateCallRequest(
        Long receiverId,
        CallType callType
) {}