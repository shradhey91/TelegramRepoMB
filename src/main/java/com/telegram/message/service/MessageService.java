package com.telegram.message.service;

import com.telegram.chat.service.ChatService;
import com.telegram.common.enums.MemberRole;
import com.telegram.message.dto.request.EditMessageRequest;
import com.telegram.message.dto.request.SendMessageRequest;
import com.telegram.message.dto.response.MessageResponse;
import com.telegram.websocket.dto.WebSocketEvent;
import com.telegram.chat.entity.Chat;
import com.telegram.chat.entity.ChatMember;
import com.telegram.message.entity.Message;
import com.telegram.message.entity.MessageEditHistory;
import com.telegram.auth.entity.User;
import com.telegram.common.enums.MessageType;
import com.telegram.common.exception.AccessDeniedException;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.chat.repository.ChatMemberRepo;
import com.telegram.chat.repository.ChatRepo;
import com.telegram.message.repository.MessageRepo;
import com.telegram.user.repository.UserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    private final MessageRepo messageRepo;
    private final ChatRepo chatRepo;
    private final ChatMemberRepo chatMemberRepo;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public MessageService(MessageRepo messageRepo, ChatRepo chatRepo,
                          ChatMemberRepo chatMemberRepo, UserRepo userRepo,
                          SimpMessagingTemplate messagingTemplate,
                          ChatService chatService) {
        this.messageRepo = messageRepo;
        this.chatRepo = chatRepo;
        this.chatMemberRepo = chatMemberRepo;
        this.userRepo = userRepo;
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Chat chat = chatRepo.findById(request.chatId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        if (!chatMemberRepo.existsByChatIdAndUserId(chat.getId(), senderId)) {
            throw new AccessDeniedException("You are not a member of this chat");
        }

        Message.MessageBuilder builder = Message.builder()
                .chat(chat)
                .sender(sender)
                .type(request.type() != null ? request.type() : MessageType.TEXT)
                .content(request.content())
                .isEdited(false)
                .isDeleted(false);

        if (request.replyToId() != null) {
            Message replyTo = messageRepo.findById(request.replyToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reply message not found"));
            builder.replyTo(replyTo);
        }

        Message message = builder.build();
        messageRepo.save(message);

        chat.setUpdatedAt(LocalDateTime.now());
        chatRepo.save(chat);

        MessageResponse response = chatService.toMessageResponse(message);

        broadcastToChat(chat.getId(), WebSocketEvent.of("NEW_MESSAGE", response));

        return response;
    }

    @Transactional
    public MessageResponse editMessage(Long userId, EditMessageRequest request) {
        Message message = messageRepo.findById(request.messageId())
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own messages");
        }

        if (message.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted message");
        }

        MessageEditHistory history = MessageEditHistory.builder()
                .message(message)
                .oldContent(message.getContent())
                .build();
        message.getEditHistory().add(history);

        message.setContent(request.content());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());
        messageRepo.save(message);

        MessageResponse response = chatService.toMessageResponse(message);

        broadcastToChat(message.getChat().getId(), WebSocketEvent.of("MESSAGE_EDITED", response));

        return response;
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            ChatMember member = chatMemberRepo.findByChatIdAndUserId(message.getChat().getId(), userId)
                    .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat"));

            if (member.getRole() != MemberRole.OWNER &&
                    member.getRole() != MemberRole.ADMIN) {
                throw new AccessDeniedException("You can only delete your own messages");
            }
        }

        message.setIsDeleted(true);
        message.setContent(null);
        messageRepo.save(message);

        broadcastToChat(message.getChat().getId(),
                WebSocketEvent.of("MESSAGE_DELETED", Map.of(
                        "messageId", messageId,
                        "chatId", message.getChat().getId())));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getChatMessages(Long chatId, Long userId, int page, int size) {
        if (!chatMemberRepo.existsByChatIdAndUserId(chatId, userId)) {
            throw new AccessDeniedException("You are not a member of this chat");
        }

        Page<Message> messages = messageRepo.findByChatId(chatId, PageRequest.of(page, size));

        return messages.getContent().stream()
                .map(chatService::toMessageResponse)
                .toList();
    }

    @Transactional
    public void markAsRead(Long chatId, Long messageId, Long userId) {
        if (!chatMemberRepo.existsByChatIdAndUserId(chatId, userId)) {
            return;
        }

        Message message = messageRepo.findById(messageId).orElse(null);
        if (message == null) return;

        ChatMember member = chatMemberRepo.findByChatIdAndUserId(chatId, userId).orElse(null);
        if (member == null) return;

        member.setLastReadMessage(message);
        chatMemberRepo.save(member);

        broadcastToChat(chatId, WebSocketEvent.of("MESSAGE_READ", Map.of(
                "chatId", chatId,
                "messageId", messageId,
                "readByUserId", userId)));
    }

    public void sendTypingIndicator(Long chatId, Long userId, boolean isTyping) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        broadcastToChat(chatId, WebSocketEvent.of("TYPING", Map.of(
                "chatId", chatId,
                "userId", userId,
                "username", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                "isTyping", isTyping)));
    }

    private void broadcastToChat(Long chatId, WebSocketEvent<?> event) {
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, event);
    }
}