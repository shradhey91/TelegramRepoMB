package com.telegram.call.service;

import com.telegram.call.dto.request.InitiateCallRequest;
import com.telegram.call.dto.response.CallResponse;
import com.telegram.notification.dto.NotificationEvent;
import com.telegram.notification.enums.NotificationType;
import com.telegram.notification.service.NotificationService;
import com.telegram.websocket.dto.WebSocketEvent;
import com.telegram.call.entity.Call;
import com.telegram.call.entity.CallParticipant;
import com.telegram.auth.entity.User;
import com.telegram.common.enums.CallStatus;
import com.telegram.common.enums.CallType;
import com.telegram.common.exception.AccessDeniedException;
import com.telegram.common.exception.ConflictException;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.call.repository.CallParticipantRepo;
import com.telegram.call.repository.CallRepo;
import com.telegram.user.repository.UserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CallService {

    private final CallRepo callRepo;
    private final CallParticipantRepo participantRepo;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public CallService(CallRepo callRepo,
                       CallParticipantRepo participantRepo,
                       UserRepo userRepo,
                       SimpMessagingTemplate messagingTemplate,
                       NotificationService notificationService) {
        this.callRepo = callRepo;
        this.participantRepo = participantRepo;
        this.userRepo = userRepo;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    @Transactional
    public CallResponse initiateCall(Long callerId, InitiateCallRequest request) {
        if (callerId.equals(request.receiverId())) {
            throw new IllegalArgumentException("You cannot call yourself");
        }

        User caller = userRepo.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("Caller not found"));

        User receiver = userRepo.findById(request.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        List<CallStatus> busyStatuses = List.of(CallStatus.RINGING, CallStatus.ACTIVE);

        if (!callRepo.findActiveCallsForUser(callerId, busyStatuses).isEmpty()) {
            throw new ConflictException("You are already in a call");
        }

        if (!callRepo.findActiveCallsForUser(request.receiverId(), busyStatuses).isEmpty()) {
            throw new ConflictException("The receiver is currently busy");
        }

        Call call = Call.builder()
                .caller(caller)
                .receiver(receiver)
                .callType(request.callType() != null ? request.callType() : CallType.VOICE)
                .status(CallStatus.RINGING)
                .build();

        callRepo.save(call);

        CallParticipant callerParticipant = CallParticipant.builder()
                .call(call)
                .user(caller)
                .cameraEnabled(request.callType() == CallType.VIDEO)
                .build();
        participantRepo.save(callerParticipant);

        CallResponse response = toCallResponse(call);

        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/calls",
                WebSocketEvent.of("INCOMING_CALL", response));

        String callerName = caller.getDisplayName() != null
                ? caller.getDisplayName() : caller.getUsername();

        notificationService.createAndSend(
                NotificationEvent.builder()
                        .recipientId(receiver.getId())
                        .actorId(caller.getId())
                        .actorName(callerName)
                        .type(NotificationType.CALL_INCOMING)
                        .referenceId(call.getId())
                        .chatId(null)
                        .content(callerName + " is calling you")
                        .build()
        );

        return response;
    }

    @Transactional
    public CallResponse acceptCall(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);

        if (!call.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("Only the receiver can accept this call");
        }

        if (call.getStatus() != CallStatus.RINGING) {
            throw new IllegalStateException("Call is not in RINGING state, current: " + call.getStatus());
        }

        call.setStatus(CallStatus.ACTIVE);
        call.setStartedAt(LocalDateTime.now());
        callRepo.save(call);

        CallParticipant receiverParticipant = CallParticipant.builder()
                .call(call)
                .user(call.getReceiver())
                .cameraEnabled(call.getCallType() == CallType.VIDEO)
                .build();
        participantRepo.save(receiverParticipant);

        CallResponse response = toCallResponse(call);

        messagingTemplate.convertAndSendToUser(
                call.getCaller().getEmail(),
                "/queue/calls",
                WebSocketEvent.of("CALL_ACCEPTED", response));

        return response;
    }

    @Transactional
    public CallResponse rejectCall(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);

        if (!call.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("Only the receiver can reject this call");
        }

        if (call.getStatus() != CallStatus.RINGING) {
            throw new IllegalStateException("Call is not in RINGING state");
        }

        call.setStatus(CallStatus.REJECTED);
        call.setEndedAt(LocalDateTime.now());
        callRepo.save(call);

        CallResponse response = toCallResponse(call);

        messagingTemplate.convertAndSendToUser(
                call.getCaller().getEmail(),
                "/queue/calls",
                WebSocketEvent.of("CALL_REJECTED", response));

        return response;
    }

    @Transactional
    public CallResponse cancelCall(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);

        if (!call.getCaller().getId().equals(userId)) {
            throw new AccessDeniedException("Only the caller can cancel this call");
        }

        if (call.getStatus() != CallStatus.RINGING) {
            throw new IllegalStateException("Call is not in RINGING state");
        }

        call.setStatus(CallStatus.CANCELLED);
        call.setEndedAt(LocalDateTime.now());
        callRepo.save(call);

        CallResponse response = toCallResponse(call);

        messagingTemplate.convertAndSendToUser(
                call.getReceiver().getEmail(),
                "/queue/calls",
                WebSocketEvent.of("CALL_CANCELLED", response));

        return response;
    }

    @Transactional
    public CallResponse endCall(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);

        if (!call.getCaller().getId().equals(userId) &&
                !call.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("You are not a participant in this call");
        }

        if (call.getStatus() != CallStatus.ACTIVE) {
            throw new IllegalStateException("Call is not active");
        }

        call.endCall();
        callRepo.save(call);

        participantRepo.findByCallIdAndLeftAtIsNull(callId)
                .forEach(p -> {
                    p.setLeftAt(LocalDateTime.now());
                    participantRepo.save(p);
                });

        CallResponse response = toCallResponse(call);

        User otherUser = call.getCaller().getId().equals(userId)
                ? call.getReceiver() : call.getCaller();

        messagingTemplate.convertAndSendToUser(
                otherUser.getEmail(),
                "/queue/calls",
                WebSocketEvent.of("CALL_ENDED", response));

        String actorName = call.getCaller().getId().equals(userId)
                ? (call.getCaller().getDisplayName() != null
                   ? call.getCaller().getDisplayName() : call.getCaller().getUsername())
                : (call.getReceiver().getDisplayName() != null
                   ? call.getReceiver().getDisplayName() : call.getReceiver().getUsername());

        notificationService.createAndSend(
                NotificationEvent.builder()
                        .recipientId(otherUser.getId())
                        .actorId(userId)
                        .actorName(actorName)
                        .type(NotificationType.CALL_ENDED)
                        .referenceId(call.getId())
                        .chatId(null)
                        .content("Call ended" + (call.getDurationSeconds() != null
                                ? " · " + call.getDurationSeconds() + "s" : ""))
                        .build()
        );

        return response;
    }

    @Transactional
    public void markAsMissed(Long callId) {
        Call call = getCallOrThrow(callId);

        if (call.getStatus() != CallStatus.RINGING) return;

        call.setStatus(CallStatus.MISSED);
        call.setEndedAt(LocalDateTime.now());
        callRepo.save(call);

        CallResponse response = toCallResponse(call);

        messagingTemplate.convertAndSendToUser(
                call.getCaller().getEmail(),
                "/queue/calls",
                WebSocketEvent.of("CALL_MISSED", response));

        messagingTemplate.convertAndSendToUser(
                call.getReceiver().getEmail(),
                "/queue/calls",
                WebSocketEvent.of("CALL_MISSED", response));

        String callerName = call.getCaller().getDisplayName() != null
                ? call.getCaller().getDisplayName() : call.getCaller().getUsername();

        notificationService.createAndSend(
                NotificationEvent.builder()
                        .recipientId(call.getReceiver().getId())
                        .actorId(call.getCaller().getId())
                        .actorName(callerName)
                        .type(NotificationType.CALL_MISSED)
                        .referenceId(call.getId())
                        .chatId(null)
                        .content("Missed call from " + callerName)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public List<CallResponse> getCallHistory(Long userId, int page, int size) {
        Page<Call> calls = callRepo.findCallHistoryByUserId(userId, PageRequest.of(page, size));
        return calls.getContent().stream()
                .map(this::toCallResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CallResponse getCallById(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);

        if (!call.getCaller().getId().equals(userId) &&
                !call.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("You are not a participant in this call");
        }

        return toCallResponse(call);
    }

    private Call getCallOrThrow(Long callId) {
        return callRepo.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
    }

    private CallResponse toCallResponse(Call call) {
        User caller = call.getCaller();
        User receiver = call.getReceiver();

        return new CallResponse(
                call.getId(),
                caller.getId(),
                caller.getDisplayName() != null ? caller.getDisplayName() : caller.getUsername(),
                caller.getAvatarUrl(),
                receiver.getId(),
                receiver.getDisplayName() != null ? receiver.getDisplayName() : receiver.getUsername(),
                receiver.getAvatarUrl(),
                call.getCallType(),
                call.getStatus(),
                call.getCreatedAt(),
                call.getStartedAt(),
                call.getEndedAt(),
                call.getDurationSeconds());
    }
}