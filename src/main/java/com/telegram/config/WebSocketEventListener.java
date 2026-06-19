package com.telegram.config;

import com.telegram.entities.User;
import com.telegram.repository.UserRepo;
import com.telegram.security.CustomUserDetails;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class WebSocketEventListener {

    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(UserRepo userRepo, SimpMessagingTemplate messagingTemplate) {
        this.userRepo = userRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnect(SessionConnectEvent event) {
        var principal = event.getUser();
        if (principal == null) return;

        Long userId = extractUserId(principal);
        if (userId == null) return;

        userRepo.findById(userId).ifPresent(user -> {
            user.setIsOnline(true);
            userRepo.save(user);

            messagingTemplate.convertAndSend("/topic/presence",
                    (Object) Map.of("userId", userId, "isOnline", true));
        });
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        var principal = event.getUser();
        if (principal == null) return;

        Long userId = extractUserId(principal);
        if (userId == null) return;

        userRepo.findById(userId).ifPresent(user -> {
            user.setIsOnline(false);
            user.setLastSeenAt(LocalDateTime.now());
            userRepo.save(user);

            messagingTemplate.convertAndSend("/topic/presence",
                    (Object) Map.of("userId", userId,
                            "isOnline", false,
                            "lastSeenAt", LocalDateTime.now().toString()));
        });
    }

    private Long extractUserId(java.security.Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails.getId();
            }
        }
        return null;
    }
}