package com.telegram.chat.controller;

import com.telegram.chat.dto.request.ChangeRoleRequest;
import com.telegram.chat.dto.request.CreateChatRequest;
import com.telegram.chat.dto.response.ChatResponse;
import com.telegram.chat.dto.response.PinnedMessageResponse;
import com.telegram.auth.security.CustomUserDetails;
import com.telegram.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@Tag(name = "Chats", description = "Create, list, and manage chats and members")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    @PostMapping
    @Operation(summary = "Create a new chat (private, group, or channel)")
    public ResponseEntity<ChatResponse> createChat(
            @RequestBody CreateChatRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.createChat(userId, request));
    }

    @GetMapping
    @Operation(summary = "Get the current user's chats (paginated)")
    public ResponseEntity<Page<ChatResponse>> getUserChats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.getUserChats(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/{chatId}")
    @Operation(summary = "Get a specific chat by ID")
    public ResponseEntity<ChatResponse> getChatById(
            @PathVariable Long chatId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.getChatById(chatId, userId));
    }

    @PostMapping("/{chatId}/members/{userId}")
    @Operation(summary = "Add a member to a group or channel")
    public ResponseEntity<Map<String, String>> addMember(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            Authentication authentication) {
        Long requesterId = extractUserId(authentication);
        chatService.addMemberToChat(chatId, userId, requesterId);
        return ResponseEntity.ok(Map.of("message", "Member added successfully"));
    }

    @DeleteMapping("/{chatId}/members/{userId}")
    @Operation(summary = "Remove a member from a chat (or leave)")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            Authentication authentication) {
        Long requesterId = extractUserId(authentication);
        chatService.removeMemberFromChat(chatId, userId, requesterId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }


    @PutMapping("/{chatId}/members/{userId}/role")
    @Operation(summary = "Change a member's role (OWNER only). Allowed roles: ADMIN, MEMBER")
    public ResponseEntity<Map<String, String>> changeMemberRole(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        chatService.changeMemberRole(chatId, userId, request.newRole(), user.getId());
        return ResponseEntity.ok(Map.of("message", "Role updated to " + request.newRole()));
    }


    @PostMapping("/join")
    @Operation(summary = "Join a group or channel via invite link")
    public ResponseEntity<ChatResponse> joinViaInviteLink(
            @RequestParam String inviteLink,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.joinViaInviteLink(userId, inviteLink));
    }

    @PostMapping("/{chatId}/invite-link/regenerate")
    @Operation(summary = "Regenerate the invite link (admin only)")
    public ResponseEntity<Map<String, String>> regenerateInviteLink(
            @PathVariable Long chatId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        String newLink = chatService.regenerateInviteLink(chatId, userId);
        return ResponseEntity.ok(Map.of("inviteLink", newLink));
    }


    @PostMapping("/{chatId}/pins/{messageId}")
    @Operation(summary = "Pin a message in a chat")
    public ResponseEntity<PinnedMessageResponse> pinMessage(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.pinMessage(chatId, messageId, userId));
    }

    @DeleteMapping("/{chatId}/pins/{messageId}")
    @Operation(summary = "Unpin a message from a chat")
    public ResponseEntity<Map<String, String>> unpinMessage(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        chatService.unpinMessage(chatId, messageId, userId);
        return ResponseEntity.ok(Map.of("message", "Message unpinned successfully"));
    }

    @GetMapping("/{chatId}/pins")
    @Operation(summary = "Get all pinned messages in a chat")
    public ResponseEntity<List<PinnedMessageResponse>> getPinnedMessages(
            @PathVariable Long chatId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.getPinnedMessages(chatId, userId));
    }

    private Long extractUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}