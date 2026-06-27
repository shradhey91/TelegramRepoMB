package com.telegram.chat.dto.response;

import com.telegram.message.dto.response.MessageResponse;

import java.time.OffsetDateTime;

public record PinnedMessageResponse(
        Long id,
        Long chatId,
        MessageResponse message,
        Long pinnedById,
        String pinnedByName,
        OffsetDateTime pinnedAt
) {}