package com.telegram.dto.request;

import com.telegram.enums.CallType;

public record InitiateCallRequest(
        Long receiverId,
        CallType callType
) {}