package com.telegram.filetransfer.controller;

import com.telegram.auth.security.CustomUserDetails;
import com.telegram.filetransfer.dto.request.InitiateFileTransferRequest;
import com.telegram.filetransfer.dto.response.FileTransferResponse;
import com.telegram.filetransfer.service.FileTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/file-transfers")
@Tag(name = "P2P File Transfers", description = "Initiate, accept, reject, and track peer-to-peer file transfers")
public class FileTransferController {

    private final FileTransferService fileTransferService;

    public FileTransferController(FileTransferService fileTransferService) {
        this.fileTransferService = fileTransferService;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a P2P file transfer — sends offer to receiver via WebSocket")
    public ResponseEntity<FileTransferResponse> initiate(
            @RequestBody InitiateFileTransferRequest request,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(fileTransferService.initiateTransfer(userId, request));
    }

    @PostMapping("/{transferId}/accept")
    @Operation(summary = "Accept a file transfer — triggers WebRTC negotiation")
    public ResponseEntity<FileTransferResponse> accept(
            @PathVariable Long transferId,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(fileTransferService.acceptTransfer(userId, transferId));
    }

    @PostMapping("/{transferId}/reject")
    @Operation(summary = "Reject a file transfer")
    public ResponseEntity<FileTransferResponse> reject(
            @PathVariable Long transferId,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(fileTransferService.rejectTransfer(userId, transferId));
    }

    @PostMapping("/{transferId}/cancel")
    @Operation(summary = "Cancel a file transfer (either party)")
    public ResponseEntity<FileTransferResponse> cancel(
            @PathVariable Long transferId,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(fileTransferService.cancelTransfer(userId, transferId));
    }

    @PostMapping("/{transferId}/complete")
    @Operation(summary = "Mark transfer as completed — creates a message in the chat")
    public ResponseEntity<FileTransferResponse> complete(
            @PathVariable Long transferId,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(fileTransferService.completeTransfer(userId, transferId));
    }

    @PostMapping("/{transferId}/failed")
    @Operation(summary = "Mark transfer as failed (connection dropped)")
    public ResponseEntity<Map<String, String>> failed(
            @PathVariable Long transferId,
            Authentication auth) {
        fileTransferService.markFailed(transferId);
        return ResponseEntity.ok(Map.of("message", "Transfer marked as failed"));
    }

    @GetMapping("/{transferId}")
    @Operation(summary = "Get file transfer details")
    public ResponseEntity<FileTransferResponse> getTransfer(
            @PathVariable Long transferId,
            Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(fileTransferService.getTransfer(userId, transferId));
    }

    private Long extractUserId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }
}