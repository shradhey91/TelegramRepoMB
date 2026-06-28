package com.telegram.notification.service;

import com.telegram.auth.entity.User;
import com.telegram.notification.dto.NotificationEvent;
import com.telegram.notification.dto.NotificationResponse;
import com.telegram.notification.entities.Notification;

import com.telegram.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.telegram.user.repository.UserRepo;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public void createAndSend(NotificationEvent event) {

        User recipient = userRepo.findById(event.recipientId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + event.recipientId()));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actorId(event.actorId())
                .actorName(event.actorName())
                .type(event.type())
                .referenceId(event.referenceId())
                .chatId(event.chatId())
                .content(event.content())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/notifications",
                toResponse(saved)
        );
    }

    public Page<NotificationResponse> getNotifications(Long userId, int page, int size) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository
                .findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .actorId(n.getActorId())
                .actorName(n.getActorName())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .chatId(n.getChatId())
                .content(n.getContent())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}