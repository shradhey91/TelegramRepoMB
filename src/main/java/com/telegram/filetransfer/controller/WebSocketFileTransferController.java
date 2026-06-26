package com.telegram.filetransfer.controller;

import com.telegram.auth.entity.User;
import com.telegram.auth.security.CustomUserDetails;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.filetransfer.dto.request.FileTransferSignal;
import com.telegram.filetransfer.service.FileTransferService;
import com.telegram.user.repository.UserRepo;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * Relays WebRTC signaling messages between peers for P2P file transfer.
 *
 * The server NEVER sees file bytes — it only forwards:
 *   - SDP_OFFER / SDP_ANSWER  (session descriptions)
 *   - ICE_CANDIDATE           (connectivity candidates)
 *   - PROGRESS                (optional % updates for UI)
 *   - CANCEL                  (abort mid-transfer)
 *
 * Frontend sends to:   /app/filetransfer.signal
 * Receiver listens on: /user/queue/file-transfer-signal
 */
@Controller
public class WebSocketFileTransferController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepo userRepo;
    private final FileTransferService fileTransferService;

    public WebSocketFileTransferController(SimpMessagingTemplate messagingTemplate,
                                           UserRepo userRepo,
                                           FileTransferService fileTransferService) {
        this.messagingTemplate = messagingTemplate;
        this.userRepo = userRepo;
        this.fileTransferService = fileTransferService;
    }

    @MessageMapping("/filetransfer.signal")
    public void handleSignal(@Payload FileTransferSignal signal, Principal principal) {
        Long senderId = extractUserId(principal);

        User receiver = userRepo.findById(signal.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        // If the DataChannel just opened, mark as transferring
        if ("DATACHANNEL_OPEN".equals(signal.type())) {
            fileTransferService.markTransferring(signal.transferId());
        }

        Map<String, Object> outbound = Map.of(
                "transferId", signal.transferId(),
                "senderId", senderId,
                "type", signal.type(),
                "payload", signal.payload() != null ? signal.payload() : ""
        );

        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/file-transfer-signal",
                outbound);
    }

    private Long extractUserId(Principal principal) {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getId();
    }
}