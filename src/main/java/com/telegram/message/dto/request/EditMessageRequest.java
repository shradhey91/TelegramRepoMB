package com.telegram.message.dto.request;

public record EditMessageRequest(
        Long messageId,
        String content
) {}