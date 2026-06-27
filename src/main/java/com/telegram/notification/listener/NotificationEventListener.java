package com.telegram.notification.listener;

import com.telegram.notification.dto.NotificationEvent;
import com.telegram.notification.enums.NotificationType;
import com.telegram.notification.service.NotificationService;
import com.telegram.chat.repository.ChatMemberRepo;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final ChatMemberRepo chatMemberRepo;

    public NotificationEventListener(NotificationService notificationService,
                                     ChatMemberRepo chatMemberRepo) {
        this.notificationService = notificationService;
        this.chatMemberRepo = chatMemberRepo;
    }


    @Async
    @EventListener
    public void onNewMessage(ChatNotificationEvent.NewMessage event) {
        notifyOtherChatMembers(
                event.chatId(),
                event.senderId(),
                event.senderName(),
                NotificationType.NEW_MESSAGE,
                event.messageId(),
                truncate(event.content(), 100)
        );
    }

    @Async
    @EventListener
    public void onMessageReply(ChatNotificationEvent.MessageReply event) {

        if (event.originalSenderId().equals(event.replierId())) return;

        notificationService.createAndSend(NotificationEvent.builder()
                .recipientId(event.originalSenderId())
                .actorId(event.replierId())
                .actorName(event.replierName())
                .type(NotificationType.REPLY)
                .referenceId(event.messageId())
                .chatId(event.chatId())
                .content(truncate(event.content(), 100))
                .build());
    }


    @Async
    @EventListener
    public void onMemberJoined(ChatNotificationEvent.MemberJoined event) {
        notifyOtherChatMembers(
                event.chatId(),
                event.userId(),
                event.userName(),
                NotificationType.USER_JOINED_CHAT,
                event.chatId(),
                event.userName() + " joined the chat"
        );
    }

    @Async
    @EventListener
    public void onMemberLeft(ChatNotificationEvent.MemberLeft event) {
        notifyOtherChatMembers(
                event.chatId(),
                event.userId(),
                event.userName(),
                NotificationType.USER_LEFT_CHAT,
                event.chatId(),
                event.userName() + " left the chat"
        );
    }


    @Async
    @EventListener
    public void onIncomingCall(ChatNotificationEvent.IncomingCall event) {
        notificationService.createAndSend(NotificationEvent.builder()
                .recipientId(event.receiverId())
                .actorId(event.callerId())
                .actorName(event.callerName())
                .type(NotificationType.CALL_INCOMING)
                .referenceId(event.callId())
                .content(event.callerName() + " is calling you")
                .build());
    }

    @Async
    @EventListener
    public void onMissedCall(ChatNotificationEvent.MissedCall event) {
        notificationService.createAndSend(NotificationEvent.builder()
                .recipientId(event.receiverId())
                .actorId(event.callerId())
                .actorName(event.callerName())
                .type(NotificationType.CALL_MISSED)
                .referenceId(event.callId())
                .content("Missed call from " + event.callerName())
                .build());
    }


    private void notifyOtherChatMembers(Long chatId, Long actorId, String actorName,
                                         NotificationType type, Long referenceId,
                                         String content) {
        List<Long> memberIds = chatMemberRepo.findUserIdsByChatId(chatId);

        for (Long memberId : memberIds) {
            if (memberId.equals(actorId)) continue;

            notificationService.createAndSend(NotificationEvent.builder()
                    .recipientId(memberId)
                    .actorId(actorId)
                    .actorName(actorName)
                    .type(type)
                    .referenceId(referenceId)
                    .chatId(chatId)
                    .content(content)
                    .build());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
