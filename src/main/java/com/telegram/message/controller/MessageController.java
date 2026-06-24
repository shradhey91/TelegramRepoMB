package com.telegram.message.controller;

import com.telegram.message.dto.request.EditMessageRequest;
import com.telegram.message.dto.request.SendMessageRequest;
import com.telegram.message.dto.response.MessageResponse;
import com.telegram.auth.security.CustomUserDetails;
import com.telegram.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Messages", description = "Send, edit, delete, and fetch messages via REST")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Send a message to a chat")
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestBody SendMessageRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(messageService.sendMessage(userId, request));
    }

    @PutMapping
    @Operation(summary = "Edit a message")
    public ResponseEntity<MessageResponse> editMessage(
            @RequestBody EditMessageRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(messageService.editMessage(userId, request));
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Delete a message")
    public ResponseEntity<Map<String, String>> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        messageService.deleteMessage(userId, messageId);
        return ResponseEntity.ok(Map.of("message", "Message deleted successfully"));
    }

    @GetMapping("/chat/{chatId}")
    @Operation(summary = "Get paginated messages for a chat (for initial load / reconnection)")
    public ResponseEntity<List<MessageResponse>> getChatMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(messageService.getChatMessages(chatId, userId, page, size));
    }

    @PostMapping("/chat/{chatId}/read/{messageId}")
    @Operation(summary = "Mark messages as read up to a given message")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        messageService.markAsRead(chatId, messageId, userId);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    private Long extractUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}