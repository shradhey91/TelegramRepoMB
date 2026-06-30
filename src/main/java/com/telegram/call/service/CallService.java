package com.telegram.call.service;

import com.telegram.call.dto.request.InitiateCallRequest;
import com.telegram.call.dto.response.CallResponse;

import com.telegram.call.dto.response.ParticipantResponse;
import com.telegram.common.enums.ParticipantRole;
import com.telegram.common.enums.ParticipantStatus;
import com.telegram.notification.listener.ChatNotificationEvent;
import com.telegram.user.service.BlockService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class CallService {

    private final CallRepo callRepo;
    private final CallParticipantRepo participantRepo;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final BlockService blockService;

    public CallService(CallRepo callRepo,
                       CallParticipantRepo participantRepo,
                       UserRepo userRepo,
                       SimpMessagingTemplate messagingTemplate,
                       ApplicationEventPublisher eventPublisher,
                       BlockService blockService) {
        this.callRepo = callRepo;
        this.participantRepo = participantRepo;
        this.userRepo = userRepo;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.blockService = blockService;
    }

    private static final List<CallStatus> BUSY_STATUSES =
            List.of(CallStatus.RINGING, CallStatus.ACTIVE);


    @Transactional
    public CallResponse initiateCall(Long creatorId, InitiateCallRequest request) {

        if (request.participantIds() == null || request.participantIds().isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }
        if (request.participantIds().contains(creatorId)) {
            throw new IllegalArgumentException("You cannot invite yourself");
        }

        User creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        if (!callRepo.findActiveCallsForUser(creatorId, BUSY_STATUSES).isEmpty()) {
            throw new ConflictException("You are already in a call");
        }

        Call call = Call.builder()
                .creator(creator)
                .callType(request.callType() != null ? request.callType() : CallType.VIDEO)
                .status(CallStatus.RINGING)
                .build();
        callRepo.save(call);

        participantRepo.save(CallParticipant.builder()
                .call(call)
                .user(creator)
                .role(ParticipantRole.CREATOR)
                .status(ParticipantStatus.JOINED)
                .cameraEnabled(call.getCallType() == CallType.VIDEO)
                .joinedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());

        String creatorName = creator.getDisplayName() != null
                ? creator.getDisplayName() : creator.getUsername();

        for (Long inviteeId : request.participantIds()) {
            if (blockService.isBlocked(creatorId, inviteeId)) {
                continue;
            }

            User invitee = userRepo.findById(inviteeId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + inviteeId));

            boolean busy = !callRepo.findActiveCallsForUser(inviteeId, BUSY_STATUSES).isEmpty();

            participantRepo.save(CallParticipant.builder()
                    .call(call)
                    .user(invitee)
                    .role(ParticipantRole.MEMBER)
                    .status(busy ? ParticipantStatus.REJECTED : ParticipantStatus.RINGING)
                    .cameraEnabled(false)
                    .build());

            if (busy) {
                continue;
            }

            messagingTemplate.convertAndSendToUser(
                    invitee.getEmail(),
                    "/queue/calls",
                    WebSocketEvent.of("INCOMING_CALL", toCallResponse(call)));

            eventPublisher.publishEvent(new ChatNotificationEvent.IncomingCall(
                    call.getId(), creatorId, creatorName, inviteeId));
        }

        return toCallResponse(call);
    }

    @Transactional
    public CallResponse acceptCall(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);
        CallParticipant p = getParticipantOrThrow(callId, userId);

        if (p.getStatus() != ParticipantStatus.RINGING) {
            throw new IllegalStateException("You are not in RINGING state, current: " + p.getStatus());
        }
        if (call.getStatus() == CallStatus.ENDED) {
            throw new IllegalStateException("Call has already ended");
        }

        p.setStatus(ParticipantStatus.JOINED);
        p.setJoinedAt(OffsetDateTime.now(ZoneOffset.UTC));
        p.setCameraEnabled(call.getCallType() == CallType.VIDEO);
        participantRepo.save(p);

        if (call.getStatus() == CallStatus.RINGING) {
            call.setStatus(CallStatus.ACTIVE);
            call.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
            callRepo.save(call);
        }

        broadcastToJoined(call, "PARTICIPANT_JOINED", toCallResponse(call), userId);
        return toCallResponse(call);
    }

    @Transactional
    public CallResponse rejectCall(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);
        CallParticipant p = getParticipantOrThrow(callId, userId);

        if (p.getStatus() != ParticipantStatus.RINGING) {
            throw new IllegalStateException("You are not in RINGING state");
        }

        p.setStatus(ParticipantStatus.REJECTED);
        p.setLeftAt(OffsetDateTime.now(ZoneOffset.UTC));
        participantRepo.save(p);

        broadcastToJoined(call, "PARTICIPANT_REJECTED", toCallResponse(call), userId);

        endCallIfNobodyLeft(call);
        return toCallResponse(call);
    }

    @Transactional
    public CallResponse cancelCall(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);

        if (!call.getCreator().getId().equals(userId)) {
            throw new AccessDeniedException("Only the creator can cancel this call");
        }
        if (call.getStatus() != CallStatus.RINGING) {
            throw new IllegalStateException("Call is not in RINGING state");
        }

        call.setStatus(CallStatus.CANCELLED);
        call.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        callRepo.save(call);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        participantRepo.findByCallId(callId).forEach(p -> {
            if (p.getStatus() == ParticipantStatus.RINGING
                    || p.getStatus() == ParticipantStatus.JOINED) {
                p.setStatus(ParticipantStatus.LEFT);
                p.setLeftAt(now);
                participantRepo.save(p);
            }
        });

        CallResponse response = toCallResponse(call);
        broadcastToAllRinging(call, "CALL_CANCELLED", response, userId);
        return response;
    }

    @Transactional
    public CallResponse endCall(Long userId, Long callId) {
        Call call = getCallOrThrow(callId);
        CallParticipant p = getParticipantOrThrow(callId, userId);

        if (p.getStatus() == ParticipantStatus.LEFT
                || p.getStatus() == ParticipantStatus.REJECTED) {
            throw new IllegalStateException("You already left this call");
        }

        p.setStatus(ParticipantStatus.LEFT);
        p.setLeftAt(OffsetDateTime.now(ZoneOffset.UTC));
        participantRepo.save(p);

        broadcastToJoined(call, "PARTICIPANT_LEFT", toCallResponse(call), userId);

        endCallIfNobodyLeft(call);
        return toCallResponse(call);
    }

    @Transactional
    public void markAsMissed(Long callId) {
        Call call = getCallOrThrow(callId);

        if (call.getStatus() != CallStatus.RINGING) {
            return;
        }

        call.setStatus(CallStatus.MISSED);
        call.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        callRepo.save(call);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String creatorName = call.getCreator().getDisplayName() != null
                ? call.getCreator().getDisplayName() : call.getCreator().getUsername();

        participantRepo.findByCallId(callId).forEach(p -> {
            if (p.getStatus() == ParticipantStatus.RINGING) {
                p.setStatus(ParticipantStatus.LEFT);
                p.setLeftAt(now);
                participantRepo.save(p);

                messagingTemplate.convertAndSendToUser(
                        p.getUser().getEmail(),
                        "/queue/calls",
                        WebSocketEvent.of("CALL_MISSED", toCallResponse(call)));

                eventPublisher.publishEvent(new ChatNotificationEvent.MissedCall(
                        call.getId(), call.getCreator().getId(), creatorName,
                        p.getUser().getId()));
            }
        });

        messagingTemplate.convertAndSendToUser(
                call.getCreator().getEmail(),
                "/queue/calls",
                WebSocketEvent.of("CALL_MISSED", toCallResponse(call)));
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

        participantRepo.findByCallIdAndUserId(callId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this call"));
        return toCallResponse(call);
    }

    private void endCallIfNobodyLeft(Call call) {
        long joined = participantRepo.countByCallIdAndStatus(call.getId(), ParticipantStatus.JOINED);

        if (joined == 0
                && call.getStatus() != CallStatus.ENDED
                && call.getStatus() != CallStatus.CANCELLED
                && call.getStatus() != CallStatus.MISSED) {

            call.endCall();
            callRepo.save(call);
            broadcastToAll(call, "CALL_ENDED", toCallResponse(call), null);
        }
    }

    private void broadcastToJoined(Call call, String event, Object payload, Long excludeUserId) {
        participantRepo.findByCallIdAndStatus(call.getId(), ParticipantStatus.JOINED)
                .stream()
                .filter(p -> excludeUserId == null || !p.getUser().getId().equals(excludeUserId))
                .forEach(p -> messagingTemplate.convertAndSendToUser(
                        p.getUser().getEmail(), "/queue/calls",
                        WebSocketEvent.of(event, payload)));
    }

    private void broadcastToAllRinging(Call call, String event, Object payload, Long excludeUserId) {
        participantRepo.findByCallIdAndStatus(call.getId(), ParticipantStatus.RINGING)
                .stream()
                .filter(p -> excludeUserId == null || !p.getUser().getId().equals(excludeUserId))
                .forEach(p -> messagingTemplate.convertAndSendToUser(
                        p.getUser().getEmail(), "/queue/calls",
                        WebSocketEvent.of(event, payload)));
    }

    private void broadcastToAll(Call call, String event, Object payload, Long excludeUserId) {
        participantRepo.findByCallId(call.getId())
                .stream()
                .filter(p -> excludeUserId == null || !p.getUser().getId().equals(excludeUserId))
                .forEach(p -> messagingTemplate.convertAndSendToUser(
                        p.getUser().getEmail(), "/queue/calls",
                        WebSocketEvent.of(event, payload)));
    }

    private Call getCallOrThrow(Long callId) {
        return callRepo.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
    }

    private CallParticipant getParticipantOrThrow(Long callId, Long userId) {
        return participantRepo.findByCallIdAndUserId(callId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant in this call"));
    }

    private CallResponse toCallResponse(Call call) {
        User creator = call.getCreator();

        List<ParticipantResponse> participants =
                participantRepo.findByCallId(call.getId()).stream()
                        .map(this::toParticipantResponse)
                        .toList();

        return new CallResponse(
                call.getId(),
                creator.getId(),
                creator.getDisplayName() != null ? creator.getDisplayName() : creator.getUsername(),
                creator.getAvatarUrl(),
                call.getCallType(),
                call.getStatus(),
                call.getCreatedAt(),
                call.getStartedAt(),
                call.getEndedAt(),
                call.getDurationSeconds(),
                participants);
    }

    private ParticipantResponse toParticipantResponse(CallParticipant p) {
        User u = p.getUser();
        return new ParticipantResponse(
                u.getId(),
                u.getDisplayName() != null ? u.getDisplayName() : u.getUsername(),
                u.getAvatarUrl(),
                p.getRole(),
                p.getStatus(),
                p.getMuted(),
                p.getCameraEnabled(),
                p.getScreenSharing());
    }
}