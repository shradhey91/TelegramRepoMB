package com.telegram.call.controller;

import com.telegram.call.dto.request.SignalingMessage;
import com.telegram.auth.entity.User;
import com.telegram.common.enums.ParticipantStatus;
import com.telegram.common.exception.AccessDeniedException;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.call.repository.CallParticipantRepo;
import com.telegram.user.repository.UserRepo;
import com.telegram.auth.security.CustomUserDetails;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketCallController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepo userRepo;
    private final CallParticipantRepo participantRepo;

    public WebSocketCallController(SimpMessagingTemplate messagingTemplate,
                                   UserRepo userRepo,
                                   CallParticipantRepo participantRepo) {
        this.messagingTemplate = messagingTemplate;
        this.userRepo = userRepo;
        this.participantRepo = participantRepo;
    }

    @MessageMapping("/call.signal")
    public void handleSignal(@Payload SignalingMessage message, Principal principal) {
        Long senderId = extractUserId(principal);

        var senderParticipant = participantRepo
                .findByCallIdAndUserId(message.callId(), senderId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this call"));
        if (senderParticipant.getStatus() != ParticipantStatus.JOINED) {
            throw new AccessDeniedException("You have not joined this call");
        }

        participantRepo
                .findByCallIdAndUserId(message.callId(), message.receiverId())
                .orElseThrow(() -> new AccessDeniedException("Target is not a participant in this call"));

        User receiver = userRepo.findById(message.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        Map<String, Object> outbound = Map.of(
                "callId", message.callId(),
                "senderId", senderId,
                "type", message.type(),
                "payload", message.payload()
        );

        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/signal",
                outbound);
    }

    private Long extractUserId(Principal principal) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getId();
    }
}