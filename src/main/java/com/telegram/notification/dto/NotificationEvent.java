package com.telegram.notification.dto;

import com.telegram.notification.enums.NotificationType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private Long recipientId;
    private Long actorId;
    private String actorName;
    private NotificationType type;
    private Long referenceId;
    private Long chatId;
    private String content;
}