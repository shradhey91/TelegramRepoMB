package com.telegram.chat.dto.response;

import com.telegram.message.dto.response.MessageResponse;

import java.time.LocalDateTime;

public record PinnedMessageResponse(
        Long id,
        Long chatId,
        MessageResponse message,
        Long pinnedById,
        String pinnedByName,
        LocalDateTime pinnedAt
) {}