package com.telegram.call.dto.request;


public record SignalingMessage(
        Long callId,
        Long receiverId,
        String type,
        String payload
) {}