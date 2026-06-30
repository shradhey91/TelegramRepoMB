package com.telegram.chat.service;

import com.telegram.chat.dto.request.CreateChatRequest;
import com.telegram.chat.dto.response.ChatMemberResponse;
import com.telegram.chat.dto.response.ChatResponse;
import com.telegram.chat.dto.response.PinnedMessageResponse;
import com.telegram.message.dto.response.AttachmentResponse;
import com.telegram.message.dto.response.MessageResponse;
import com.telegram.chat.entity.Chat;
import com.telegram.chat.entity.ChatMember;
import com.telegram.chat.entity.PinnedMessage;
import com.telegram.message.entity.Message;
import com.telegram.auth.entity.User;
import com.telegram.common.enums.ChatType;
import com.telegram.common.enums.MemberRole;
import com.telegram.common.exception.AccessDeniedException;
import com.telegram.common.exception.ConflictException;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.chat.repository.ChatMemberRepo;
import com.telegram.chat.repository.ChatRepo;
import com.telegram.chat.repository.PinnedMessageRepo;
import com.telegram.message.repository.MessageRepo;
import com.telegram.user.repository.UserRepo;
import com.telegram.websocket.dto.WebSocketEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import com.telegram.notification.listener.ChatNotificationEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepo chatRepo;
    private final ChatMemberRepo chatMemberRepo;
    private final MessageRepo messageRepo;
    private final UserRepo userRepo;
    private final PinnedMessageRepo pinnedMessageRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public ChatService(ChatRepo chatRepo, ChatMemberRepo chatMemberRepo,
                       MessageRepo messageRepo, UserRepo userRepo,
                       PinnedMessageRepo pinnedMessageRepo,
                       ApplicationEventPublisher eventPublisher,
                       SimpMessagingTemplate messagingTemplate) {
        this.chatRepo = chatRepo;
        this.chatMemberRepo = chatMemberRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.pinnedMessageRepo = pinnedMessageRepo;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
    }



    @Transactional
    public ChatResponse createChat(Long creatorId, CreateChatRequest request) {
        User creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.type() == ChatType.PRIVATE) {
            return createPrivateChat(creator, request);
        }

        return createGroupOrChannel(creator, request);
    }

    private ChatResponse createPrivateChat(User creator, CreateChatRequest request) {
        if (request.memberIds() == null || request.memberIds().size() != 1) {
            throw new IllegalArgumentException("Private chat requires exactly one other member");
        }

        Long otherUserId = request.memberIds().get(0);

        var existing = chatRepo.findPrivateChatBetween(creator.getId(), otherUserId);
        if (existing.isPresent()) {
            return toChatResponse(existing.get(), creator.getId(), null);
        }

        User otherUser = userRepo.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + otherUserId));

        Chat chat = Chat.builder()
                .type(ChatType.PRIVATE)
                .createdBy(creator)
                .build();

        chatRepo.save(chat);

        addMember(chat, creator, MemberRole.MEMBER);
        addMember(chat, otherUser, MemberRole.MEMBER);

        return toChatResponse(chat, creator.getId(), null);
    }

    private ChatResponse createGroupOrChannel(User creator, CreateChatRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Group/Channel must have a title");
        }

        Chat chat = Chat.builder()
                .type(request.type())
                .title(request.title().trim())
                .description(request.description())
                .createdBy(creator)
                .inviteLink(UUID.randomUUID().toString())
                .build();

        chatRepo.save(chat);

        addMember(chat, creator, MemberRole.OWNER);

        if (request.memberIds() != null) {
            for (Long memberId : request.memberIds()) {
                User member = userRepo.findById(memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + memberId));
                addMember(chat, member, MemberRole.MEMBER);
            }
        }

        return toChatResponse(chat, creator.getId(), null);
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getUserChats(Long userId) {
        return getUserChats(userId, PageRequest.of(0, 50)).getContent();
    }

    @Transactional(readOnly = true)
    public Page<ChatResponse> getUserChats(Long userId, Pageable pageable) {
        Page<Chat> chatPage = chatRepo.findChatsByUserId(userId, pageable);
        List<Chat> chats = chatPage.getContent();

        List<Long> chatIds = chats.stream().map(Chat::getId).toList();
        Map<Long, Message> lastMessageMap = messageRepo.findLastMessagesByChatIds(chatIds)
                .stream()
                .collect(Collectors.toMap(m -> m.getChat().getId(), Function.identity()));

        return chatPage.map(chat -> toChatResponse(chat, userId, lastMessageMap.get(chat.getId())));
    }

    @Transactional(readOnly = true)
    public ChatResponse getChatById(Long chatId, Long userId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        if (!chatMemberRepo.existsByChatIdAndUserId(chatId, userId)) {
            throw new AccessDeniedException("You are not a member of this chat");
        }

        return toChatResponse(chat, userId, null);
    }

    @Transactional
    public void addMemberToChat(Long chatId, Long userId, Long requesterId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        ChatMember requester = chatMemberRepo.findByChatIdAndUserId(chatId, requesterId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat"));

        if (requester.getRole() != MemberRole.OWNER && requester.getRole() != MemberRole.ADMIN) {
            throw new AccessDeniedException("Only admins can add members");
        }

        if (chatMemberRepo.existsByChatIdAndUserId(chatId, userId)) {
            throw new ConflictException("User is already a member");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        addMember(chat, user, MemberRole.MEMBER);
    }

    @Transactional
    public void removeMemberFromChat(Long chatId, Long userId, Long requesterId) {
        chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        ChatMember requester = chatMemberRepo.findByChatIdAndUserId(chatId, requesterId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat"));

        if (!userId.equals(requesterId) &&
                requester.getRole() != MemberRole.OWNER && requester.getRole() != MemberRole.ADMIN) {
            throw new AccessDeniedException("Only admins can remove members");
        }

        ChatMember member = chatMemberRepo.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this chat"));

        chatMemberRepo.delete(member);

        User removedUser = member.getUser();
        String leftName = removedUser.getDisplayName() != null
                ? removedUser.getDisplayName() : removedUser.getUsername();
        eventPublisher.publishEvent(new ChatNotificationEvent.MemberLeft(
                chatId, userId, leftName));
    }

    private void addMember(Chat chat, User user, MemberRole role) {
        ChatMember member = ChatMember.builder()
                .chat(chat)
                .user(user)
                .role(role)
                .build();
        chatMemberRepo.save(member);
        chat.getMembers().add(member);
    }



    @Transactional
    public ChatResponse joinViaInviteLink(Long userId, String inviteLink) {
        Chat chat = chatRepo.findByInviteLink(inviteLink)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired invite link"));

        if (chat.getType() == ChatType.PRIVATE) {
            throw new IllegalArgumentException("Cannot join a private chat via invite link");
        }

        if (chatMemberRepo.existsByChatIdAndUserId(chat.getId(), userId)) {

            return toChatResponse(chat, userId, null);
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        addMember(chat, user, MemberRole.MEMBER);


        String joinedName = user.getDisplayName() != null
                ? user.getDisplayName() : user.getUsername();

        broadcastToChat(chat.getId(), WebSocketEvent.of("MEMBER_JOINED", Map.of(
                "chatId", chat.getId(),
                "userId", user.getId(),
                "username", joinedName)));

        eventPublisher.publishEvent(new ChatNotificationEvent.MemberJoined(
                chat.getId(), userId, joinedName));

        return toChatResponse(chat, userId, null);
    }


    @Transactional
    public void changeMemberRole(Long chatId, Long targetUserId, MemberRole newRole, Long requesterId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        if (chat.getType() == ChatType.PRIVATE) {
            throw new IllegalArgumentException("Cannot change roles in a private chat");
        }

        ChatMember requester = chatMemberRepo.findByChatIdAndUserId(chatId, requesterId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat"));

        if (requester.getRole() != MemberRole.OWNER) {
            throw new AccessDeniedException("Only the group owner can change member roles");
        }

        if (targetUserId.equals(requesterId)) {
            throw new IllegalArgumentException("You cannot change your own role");
        }

        if (newRole == MemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot promote to OWNER. Use transfer ownership instead.");
        }

        ChatMember target = chatMemberRepo.findByChatIdAndUserId(chatId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this chat"));

        target.setRole(newRole);
        chatMemberRepo.save(target);

        String action = newRole == MemberRole.ADMIN ? "MEMBER_PROMOTED" : "MEMBER_DEMOTED";
        User targetUser = target.getUser();
        String targetName = targetUser.getDisplayName() != null
                ? targetUser.getDisplayName() : targetUser.getUsername();

        broadcastToChat(chatId, WebSocketEvent.of(action, Map.of(
                "chatId", chatId,
                "userId", targetUserId,
                "username", targetName,
                "newRole", newRole.name())));
    }


    @Transactional
    public String regenerateInviteLink(Long chatId, Long requesterId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        if (chat.getType() == ChatType.PRIVATE) {
            throw new IllegalArgumentException("Private chats do not have invite links");
        }

        ChatMember requester = chatMemberRepo.findByChatIdAndUserId(chatId, requesterId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat"));

        if (requester.getRole() != MemberRole.OWNER && requester.getRole() != MemberRole.ADMIN) {
            throw new AccessDeniedException("Only admins can regenerate invite links");
        }

        String newLink = UUID.randomUUID().toString();
        chat.setInviteLink(newLink);
        chatRepo.save(chat);

        return newLink;
    }

    @Transactional
    public PinnedMessageResponse pinMessage(Long chatId, Long messageId, Long userId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        ChatMember member = chatMemberRepo.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat"));

        if (chat.getType() != ChatType.PRIVATE &&
                member.getRole() != MemberRole.OWNER &&
                member.getRole() != MemberRole.ADMIN) {
            throw new AccessDeniedException("Only admins can pin messages");
        }

        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getChat().getId().equals(chatId)) {
            throw new IllegalArgumentException("Message does not belong to this chat");
        }

        if (message.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot pin a deleted message");
        }

        if (pinnedMessageRepo.existsByChatIdAndMessageId(chatId, messageId)) {
            throw new ConflictException("Message is already pinned");
        }

        User pinner = member.getUser();

        PinnedMessage pinned = PinnedMessage.builder()
                .chat(chat)
                .message(message)
                .pinnedBy(pinner)
                .build();

        pinnedMessageRepo.save(pinned);

        PinnedMessageResponse response = toPinnedMessageResponse(pinned);

        broadcastToChat(chatId, WebSocketEvent.of("MESSAGE_PINNED", response));

        return response;
    }

    @Transactional
    public void unpinMessage(Long chatId, Long messageId, Long userId) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        ChatMember member = chatMemberRepo.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat"));

        if (chat.getType() != ChatType.PRIVATE &&
                member.getRole() != MemberRole.OWNER &&
                member.getRole() != MemberRole.ADMIN) {
            throw new AccessDeniedException("Only admins can unpin messages");
        }

        PinnedMessage pinned = pinnedMessageRepo.findByChatIdAndMessageId(chatId, messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message is not pinned"));

        pinnedMessageRepo.delete(pinned);

        broadcastToChat(chatId, WebSocketEvent.of("MESSAGE_UNPINNED", Map.of(
                "chatId", chatId,
                "messageId", messageId)));
    }

    @Transactional(readOnly = true)
    public List<PinnedMessageResponse> getPinnedMessages(Long chatId, Long userId) {
        if (!chatMemberRepo.existsByChatIdAndUserId(chatId, userId)) {
            throw new AccessDeniedException("You are not a member of this chat");
        }

        return pinnedMessageRepo.findByChatIdOrderByPinnedAtDesc(chatId)
                .stream()
                .map(this::toPinnedMessageResponse)
                .toList();
    }



    private PinnedMessageResponse toPinnedMessageResponse(PinnedMessage pm) {
        User pinner = pm.getPinnedBy();
        return new PinnedMessageResponse(
                pm.getId(),
                pm.getChat().getId(),
                toMessageResponse(pm.getMessage()),
                pinner.getId(),
                pinner.getDisplayName() != null ? pinner.getDisplayName() : pinner.getUsername(),
                pm.getPinnedAt());
    }

    public ChatResponse toChatResponse(Chat chat, Long currentUserId, Message preloadedLastMessage) {
        MessageResponse lastMsg = null;

        if (preloadedLastMessage != null) {
            lastMsg = toMessageResponse(preloadedLastMessage);
        } else {
            lastMsg = messageRepo.findLastMessageByChatId(chat.getId())
                    .map(this::toMessageResponse)
                    .orElse(null);
        }

        List<ChatMemberResponse> memberResponses = chat.getMembers().stream()
                .map(m -> new ChatMemberResponse(
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        m.getUser().getDisplayName(),
                        m.getUser().getAvatarUrl(),
                        m.getRole(),
                        m.getUser().getIsOnline(),
                        m.getJoinedAt()))
                .toList();

        long unread = 0;
        ChatMember currentMember = chat.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        if (currentMember != null) {
            if (currentMember.getLastReadMessage() != null) {
                unread = messageRepo.countUnreadMessages(
                        chat.getId(),
                        currentMember.getLastReadMessage().getId(),
                        currentUserId);
            } else {
                unread = messageRepo.countAllUnreadMessages(chat.getId(), currentUserId);
            }
        }

        String title = chat.getTitle();
        if (chat.getType() == ChatType.PRIVATE && title == null) {
            title = chat.getMembers().stream()
                    .filter(m -> !m.getUser().getId().equals(currentUserId))
                    .map(m -> m.getUser().getDisplayName() != null
                            ? m.getUser().getDisplayName()
                            : m.getUser().getUsername())
                    .findFirst()
                    .orElse("Private Chat");
        }

        return new ChatResponse(
                chat.getId(),
                chat.getType(),
                title,
                chat.getDescription(),
                chat.getAvatarUrl(),
                chat.getInviteLink(),
                chat.getCreatedBy().getId(),
                chat.getCreatedAt(),
                memberResponses,
                lastMsg,
                unread);
    }

    public MessageResponse toMessageResponse(Message msg) {

        String replyContent = null;
        Long replyToId = null;

        if (msg.getReplyTo() != null) {
            replyToId = msg.getReplyTo().getId();
            replyContent = msg.getReplyTo().getIsDeleted()
                    ? null
                    : msg.getReplyTo().getContent();
        }

        List<AttachmentResponse> attachments = msg.getAttachments()
                .stream()
                .map(attachment -> new AttachmentResponse(
                        attachment.getId(),
                        "/api/attachments/" + attachment.getId(),
                        attachment.getFileName(),
                        attachment.getFileSize(),
                        attachment.getMimeType(),
                        attachment.getThumbnailUrl()
                ))
                .toList();

        return new MessageResponse(
                msg.getId(),
                msg.getChat().getId(),
                msg.getSender().getId(),
                msg.getSender().getDisplayName() != null
                        ? msg.getSender().getDisplayName()
                        : msg.getSender().getUsername(),
                msg.getSender().getAvatarUrl(),
                msg.getType(),
                msg.getIsDeleted() ? null : msg.getContent(),
                replyToId,
                replyContent,
                msg.getIsEdited(),
                msg.getCreatedAt(),
                msg.getEditedAt(),
                attachments
        );
    }

    private void broadcastToChat(Long chatId, WebSocketEvent<?> event) {
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, event);
    }
}