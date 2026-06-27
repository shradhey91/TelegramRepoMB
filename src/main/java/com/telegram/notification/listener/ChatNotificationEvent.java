package com.telegram.notification.listener;

public sealed interface ChatNotificationEvent {

    record NewMessage(
            Long chatId,
            Long senderId,
            String senderName,
            Long messageId,
            String content
    ) implements ChatNotificationEvent {}

    record MessageReply(
            Long chatId,
            Long replierId,
            String replierName,
            Long originalSenderId,
            Long messageId,
            String content
    ) implements ChatNotificationEvent {}

    record MemberJoined(
            Long chatId,
            Long userId,
            String userName
    ) implements ChatNotificationEvent {}

    record MemberLeft(
            Long chatId,
            Long userId,
            String userName
    ) implements ChatNotificationEvent {}

    record IncomingCall(
            Long callId,
            Long callerId,
            String callerName,
            Long receiverId
    ) implements ChatNotificationEvent {}

    record MissedCall(
            Long callId,
            Long callerId,
            String callerName,
            Long receiverId
    ) implements ChatNotificationEvent {}
}
