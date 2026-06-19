package com.telegram.controller;

import com.telegram.dto.request.EditMessageRequest;
import com.telegram.dto.request.SendMessageRequest;
import com.telegram.dto.response.MessageResponse;
import com.telegram.security.CustomUserDetails;
import com.telegram.services.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketChatController {

    private final MessageService messageService;

    public WebSocketChatController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        messageService.sendMessage(userId, request);
    }

    @MessageMapping("/chat.edit")
    public void editMessage(@Payload EditMessageRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        messageService.editMessage(userId, request);
    }

    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload Map<String, Long> request, Principal principal) {
        Long userId = extractUserId(principal);
        messageService.deleteMessage(userId, request.get("messageId"));
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> request, Principal principal) {
        Long userId = extractUserId(principal);
        Long chatId = ((Number) request.get("chatId")).longValue();
        boolean isTyping = (boolean) request.get("isTyping");
        messageService.sendTypingIndicator(chatId, userId, isTyping);
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, Long> request, Principal principal) {
        Long userId = extractUserId(principal);
        messageService.markAsRead(request.get("chatId"), request.get("messageId"), userId);
    }

    private Long extractUserId(Principal principal) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getId();
    }
}