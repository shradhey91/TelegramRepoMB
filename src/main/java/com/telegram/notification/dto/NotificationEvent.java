package com.telegram.notification.dto;

import com.telegram.notification.enums.NotificationType;

public record NotificationEvent(
        Long recipientId,
        Long actorId,
        String actorName,
        NotificationType type,
        Long referenceId,
        Long chatId,
        String content
) {
}