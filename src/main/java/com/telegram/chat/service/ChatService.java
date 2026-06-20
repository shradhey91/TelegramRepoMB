package com.telegram.chat.service;

import com.telegram.chat.dto.request.CreateChatRequest;
import com.telegram.chat.dto.response.ChatMemberResponse;
import com.telegram.chat.dto.response.ChatResponse;
import com.telegram.message.dto.response.MessageResponse;
import com.telegram.chat.entity.Chat;
import com.telegram.chat.entity.ChatMember;
import com.telegram.message.entity.Message;
import com.telegram.auth.entity.User;
import com.telegram.common.enums.ChatType;
import com.telegram.common.enums.MemberRole;
import com.telegram.common.exception.AccessDeniedException;
import com.telegram.common.exception.ConflictException;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.chat.repository.ChatMemberRepo;
import com.telegram.chat.repository.ChatRepo;
import com.telegram.message.repository.MessageRepo;
import com.telegram.user.repository.UserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public ChatService(ChatRepo chatRepo, ChatMemberRepo chatMemberRepo,
                       MessageRepo messageRepo, UserRepo userRepo) {
        this.chatRepo = chatRepo;
        this.chatMemberRepo = chatMemberRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
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
            return toChatResponse(existing.get(), creator.getId());
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

        return toChatResponse(chat, creator.getId());
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

        return toChatResponse(chat, creator.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getUserChats(Long userId) {
        return getUserChats(userId, PageRequest.of(0, 50)).getContent();
    }

    @Transactional(readOnly = true)
    public Page<ChatResponse> getUserChats(Long userId, Pageable pageable) {
        Page<Chat> chatPage = chatRepo.findChatsByUserId(userId, pageable);
        List<Chat> chats = chatPage.getContent();

        // Batch-load last messages for all chats in one query
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

        return toChatResponse(chat, userId);
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

    public ChatResponse toChatResponse(Chat chat, Long currentUserId) {
        MessageResponse lastMsg = messageRepo.findLastMessageByChatId(chat.getId())
                .map(this::toMessageResponse)
                .orElse(null);
        return toChatResponse(chat, currentUserId, null, lastMsg);
    }

    public ChatResponse toChatResponse(Chat chat, Long currentUserId, Message preloadedLastMessage) {
        MessageResponse lastMsg = preloadedLastMessage != null
                ? toMessageResponse(preloadedLastMessage)
                : null;
        return toChatResponse(chat, currentUserId, null, lastMsg);
    }

    private ChatResponse toChatResponse(Chat chat, Long currentUserId, Void ignored, MessageResponse lastMsg) {
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

        if (currentMember != null && currentMember.getLastReadMessage() != null) {
            unread = messageRepo.countUnreadMessages(
                    chat.getId(),
                    currentMember.getLastReadMessage().getId(),
                    currentUserId);
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
            // Don't leak content of deleted messages through reply chains
            replyContent = msg.getReplyTo().getIsDeleted()
                    ? null
                    : msg.getReplyTo().getContent();
        }

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
                msg.getEditedAt());
    }
}