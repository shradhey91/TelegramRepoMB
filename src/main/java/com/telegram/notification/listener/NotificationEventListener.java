package com.telegram.notification.listener;

import com.telegram.notification.dto.NotificationEvent;
import com.telegram.notification.enums.NotificationType;
import com.telegram.notification.service.NotificationService;
import com.telegram.chat.repository.ChatMemberRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final ChatMemberRepo chatMemberRepo;

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

        notificationService.createAndSend(
                new NotificationEvent(
                        event.originalSenderId(),
                        event.replierId(),
                        event.replierName(),
                        NotificationType.REPLY,
                        event.messageId(),
                        event.chatId(),
                        truncate(event.content(), 100)
                )
        );
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
        notificationService.createAndSend(
                new NotificationEvent(
                        event.receiverId(),
                        event.callerId(),
                        event.callerName(),
                        NotificationType.CALL_INCOMING,
                        event.callId(),
                        null,
                        event.callerName() + " is calling you"
                )
        );
    }

    @Async
    @EventListener
    public void onMissedCall(ChatNotificationEvent.MissedCall event) {
        notificationService.createAndSend(
                new NotificationEvent(
                        event.receiverId(),
                        event.callerId(),
                        event.callerName(),
                        NotificationType.CALL_MISSED,
                        event.callId(),
                        null,
                        "Missed call from " + event.callerName()
                )
        );
    }


    private void notifyOtherChatMembers(Long chatId, Long actorId, String actorName,
                                         NotificationType type, Long referenceId,
                                         String content) {
        List<Long> memberIds = chatMemberRepo.findUserIdsByChatId(chatId);

        for (Long memberId : memberIds) {
            if (memberId.equals(actorId)) continue;

            notificationService.createAndSend(
                    new NotificationEvent(
                            memberId,
                            actorId,
                            actorName,
                            type,
                            referenceId,
                            chatId,
                            content
                    )
            );
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
