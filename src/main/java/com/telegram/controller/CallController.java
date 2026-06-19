package com.telegram.controller;

import com.telegram.dto.request.InitiateCallRequest;
import com.telegram.dto.response.CallResponse;
import com.telegram.security.CustomUserDetails;
import com.telegram.services.CallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
@Tag(name = "Calls", description = "Voice and video call management")
public class CallController {

    private final CallService callService;

    public CallController(CallService callService) {
        this.callService = callService;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Start a voice or video call")
    public ResponseEntity<CallResponse> initiateCall(
            @RequestBody InitiateCallRequest request,
            Authentication authentication) {
        Long callerId = extractUserId(authentication);
        return ResponseEntity.ok(callService.initiateCall(callerId, request));
    }

    @PostMapping("/{callId}/accept")
    @Operation(summary = "Accept an incoming call")
    public ResponseEntity<CallResponse> acceptCall(
            @PathVariable Long callId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(callService.acceptCall(userId, callId));
    }

    @PostMapping("/{callId}/reject")
    @Operation(summary = "Reject an incoming call")
    public ResponseEntity<CallResponse> rejectCall(
            @PathVariable Long callId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(callService.rejectCall(userId, callId));
    }

    @PostMapping("/{callId}/cancel")
    @Operation(summary = "Cancel an outgoing call before it is answered")
    public ResponseEntity<CallResponse> cancelCall(
            @PathVariable Long callId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(callService.cancelCall(userId, callId));
    }

    @PostMapping("/{callId}/end")
    @Operation(summary = "End an active call")
    public ResponseEntity<CallResponse> endCall(
            @PathVariable Long callId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(callService.endCall(userId, callId));
    }

    @GetMapping("/{callId}")
    @Operation(summary = "Get call details by ID")
    public ResponseEntity<CallResponse> getCall(
            @PathVariable Long callId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(callService.getCallById(userId, callId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get call history for the current user")
    public ResponseEntity<List<CallResponse>> getCallHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(callService.getCallHistory(userId, page, size));
    }

    private Long extractUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}