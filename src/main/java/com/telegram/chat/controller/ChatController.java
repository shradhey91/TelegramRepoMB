package com.telegram.chat.controller;

import com.telegram.chat.dto.request.CreateChatRequest;
import com.telegram.chat.dto.response.ChatResponse;
import com.telegram.auth.security.CustomUserDetails;
import com.telegram.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    private Long extractUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}