package com.telegram.notification.listener;

/**
 * Spring application events published by existing services.
 * The NotificationEventListener picks these up and creates
 * persistent notifications + WebSocket pushes.
 *
 * Using records as events keeps them lightweight and immutable.
 * Services publish via: applicationEventPublisher.publishEvent(new ChatNotificationEvent.NewMessage(...))
 */
public sealed interface ChatNotificationEvent {

    // ── Message events ──

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

    // ── Chat membership events ──

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

    // ── Call events ──

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
