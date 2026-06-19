package com.telegram.dto.request;

public record EditMessageRequest(
        Long messageId,
        String content
) {}