package com.telegram.notification.dto;

import com.telegram.notification.enums.NotificationType;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long actorId;
    private String actorName;
    private NotificationType type;
    private Long referenceId;
    private Long chatId;
    private String content;
    private Boolean isRead;
    private OffsetDateTime createdAt;
}