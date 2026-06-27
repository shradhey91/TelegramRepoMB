package com.telegram.filetransfer.service;

import com.telegram.auth.entity.User;
import com.telegram.chat.entity.Chat;
import com.telegram.common.enums.FileTransferStatus;
import com.telegram.common.enums.MessageType;
import com.telegram.common.exception.AccessDeniedException;
import com.telegram.common.exception.ConflictException;
import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.chat.repository.ChatMemberRepo;
import com.telegram.chat.repository.ChatRepo;
import com.telegram.filetransfer.dto.request.InitiateFileTransferRequest;
import com.telegram.filetransfer.dto.response.FileTransferResponse;
import com.telegram.filetransfer.entity.FileTransfer;
import com.telegram.filetransfer.repository.FileTransferRepo;
import com.telegram.message.entity.Message;
import com.telegram.message.repository.MessageRepo;
import com.telegram.user.repository.UserRepo;
import com.telegram.websocket.dto.WebSocketEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class FileTransferService {

    private final FileTransferRepo fileTransferRepo;
    private final ChatRepo chatRepo;
    private final ChatMemberRepo chatMemberRepo;
    private final UserRepo userRepo;
    private final MessageRepo messageRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public FileTransferService(FileTransferRepo fileTransferRepo,
                               ChatRepo chatRepo,
                               ChatMemberRepo chatMemberRepo,
                               UserRepo userRepo,
                               MessageRepo messageRepo,
                               SimpMessagingTemplate messagingTemplate) {
        this.fileTransferRepo = fileTransferRepo;
        this.chatRepo = chatRepo;
        this.chatMemberRepo = chatMemberRepo;
        this.userRepo = userRepo;
        this.messageRepo = messageRepo;
        this.messagingTemplate = messagingTemplate;
    }


    @Transactional
    public FileTransferResponse initiateTransfer(Long senderId, InitiateFileTransferRequest request) {
        if (senderId.equals(request.receiverId())) {
            throw new IllegalArgumentException("Cannot send a file to yourself");
        }

        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        User receiver = userRepo.findById(request.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        Chat chat = chatRepo.findById(request.chatId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        if (!chatMemberRepo.existsByChatIdAndUserId(chat.getId(), senderId)) {
            throw new AccessDeniedException("You are not a member of this chat");
        }
        if (!chatMemberRepo.existsByChatIdAndUserId(chat.getId(), request.receiverId())) {
            throw new AccessDeniedException("Receiver is not a member of this chat");
        }

        List<FileTransferStatus> activeStatuses = List.of(
                FileTransferStatus.PENDING,
                FileTransferStatus.ACCEPTED,
                FileTransferStatus.TRANSFERRING);

        List<FileTransfer> activeTransfers = fileTransferRepo
                .findActiveTransfersForUser(senderId, activeStatuses);

        boolean alreadyTransferring = activeTransfers.stream()
                .anyMatch(ft -> ft.getReceiver().getId().equals(request.receiverId()));

        if (alreadyTransferring) {
            throw new ConflictException("You already have an active file transfer with this user");
        }

        FileTransfer transfer = FileTransfer.builder()
                .chat(chat)
                .sender(sender)
                .receiver(receiver)
                .fileName(request.fileName())
                .fileSize(request.fileSize())
                .mimeType(request.mimeType())
                .checksum(request.checksum())
                .status(FileTransferStatus.PENDING)
                .build();

        fileTransferRepo.save(transfer);

        FileTransferResponse response = toResponse(transfer);

        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/file-transfers",
                WebSocketEvent.of("FILE_TRANSFER_OFFER", response));

        return response;
    }


    @Transactional
    public FileTransferResponse acceptTransfer(Long userId, Long transferId) {
        FileTransfer transfer = getTransferOrThrow(transferId);

        if (!transfer.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("Only the receiver can accept this transfer");
        }

        if (transfer.getStatus() != FileTransferStatus.PENDING) {
            throw new IllegalStateException("Transfer is not in PENDING state");
        }

        transfer.setStatus(FileTransferStatus.ACCEPTED);
        transfer.setAcceptedAt(OffsetDateTime.now(ZoneOffset.UTC));
        fileTransferRepo.save(transfer);

        FileTransferResponse response = toResponse(transfer);

        messagingTemplate.convertAndSendToUser(
                transfer.getSender().getEmail(),
                "/queue/file-transfers",
                WebSocketEvent.of("FILE_TRANSFER_ACCEPTED", response));

        return response;
    }


    @Transactional
    public FileTransferResponse rejectTransfer(Long userId, Long transferId) {
        FileTransfer transfer = getTransferOrThrow(transferId);

        if (!transfer.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("Only the receiver can reject this transfer");
        }

        if (transfer.getStatus() != FileTransferStatus.PENDING) {
            throw new IllegalStateException("Transfer is not in PENDING state");
        }

        transfer.setStatus(FileTransferStatus.REJECTED);
        fileTransferRepo.save(transfer);

        FileTransferResponse response = toResponse(transfer);

        messagingTemplate.convertAndSendToUser(
                transfer.getSender().getEmail(),
                "/queue/file-transfers",
                WebSocketEvent.of("FILE_TRANSFER_REJECTED", response));

        return response;
    }


    @Transactional
    public FileTransferResponse cancelTransfer(Long userId, Long transferId) {
        FileTransfer transfer = getTransferOrThrow(transferId);

        if (!transfer.getSender().getId().equals(userId) &&
                !transfer.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("You are not a participant in this transfer");
        }

        if (transfer.getStatus() == FileTransferStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed transfer");
        }

        transfer.setStatus(FileTransferStatus.CANCELLED);
        fileTransferRepo.save(transfer);

        FileTransferResponse response = toResponse(transfer);

        User other = transfer.getSender().getId().equals(userId)
                ? transfer.getReceiver() : transfer.getSender();

        messagingTemplate.convertAndSendToUser(
                other.getEmail(),
                "/queue/file-transfers",
                WebSocketEvent.of("FILE_TRANSFER_CANCELLED", response));

        return response;
    }


    @Transactional
    public void markTransferring(Long transferId) {
        FileTransfer transfer = getTransferOrThrow(transferId);
        if (transfer.getStatus() == FileTransferStatus.ACCEPTED) {
            transfer.setStatus(FileTransferStatus.TRANSFERRING);
            fileTransferRepo.save(transfer);
        }
    }


    @Transactional
    public FileTransferResponse completeTransfer(Long userId, Long transferId) {
        FileTransfer transfer = getTransferOrThrow(transferId);

        if (!transfer.getSender().getId().equals(userId) &&
                !transfer.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("You are not a participant in this transfer");
        }

        transfer.setStatus(FileTransferStatus.COMPLETED);
        transfer.setBytesTransferred(transfer.getFileSize());
        transfer.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        fileTransferRepo.save(transfer);

        Message message = Message.builder()
                .chat(transfer.getChat())
                .sender(transfer.getSender())
                .type(MessageType.FILE)
                .content(buildFileTransferMessage(transfer))
                .isEdited(false)
                .isDeleted(false)
                .build();
        messageRepo.save(message);

        FileTransferResponse response = toResponse(transfer);

        messagingTemplate.convertAndSendToUser(
                transfer.getSender().getEmail(),
                "/queue/file-transfers",
                WebSocketEvent.of("FILE_TRANSFER_COMPLETED", response));

        messagingTemplate.convertAndSendToUser(
                transfer.getReceiver().getEmail(),
                "/queue/file-transfers",
                WebSocketEvent.of("FILE_TRANSFER_COMPLETED", response));

        messagingTemplate.convertAndSend(
                "/topic/chat/" + transfer.getChat().getId(),
                WebSocketEvent.of("NEW_MESSAGE", response));

        return response;
    }


    @Transactional
    public void markFailed(Long transferId) {
        FileTransfer transfer = getTransferOrThrow(transferId);
        if (transfer.getStatus() != FileTransferStatus.COMPLETED) {
            transfer.setStatus(FileTransferStatus.FAILED);
            fileTransferRepo.save(transfer);
        }
    }


    @Transactional(readOnly = true)
    public FileTransferResponse getTransfer(Long userId, Long transferId) {
        FileTransfer transfer = getTransferOrThrow(transferId);

        if (!transfer.getSender().getId().equals(userId) &&
                !transfer.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("You are not a participant in this transfer");
        }

        return toResponse(transfer);
    }


    private FileTransfer getTransferOrThrow(Long id) {
        return fileTransferRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File transfer not found"));
    }

    private String buildFileTransferMessage(FileTransfer ft) {
        String size = formatFileSize(ft.getFileSize());
        return String.format("[P2P File] %s (%s)", ft.getFileName(), size);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private FileTransferResponse toResponse(FileTransfer ft) {
        User sender = ft.getSender();
        User receiver = ft.getReceiver();

        return new FileTransferResponse(
                ft.getId(),
                ft.getChat().getId(),
                sender.getId(),
                sender.getDisplayName() != null ? sender.getDisplayName() : sender.getUsername(),
                receiver.getId(),
                receiver.getDisplayName() != null ? receiver.getDisplayName() : receiver.getUsername(),
                ft.getFileName(),
                ft.getFileSize(),
                ft.getMimeType(),
                ft.getChecksum(),
                ft.getStatus(),
                ft.getBytesTransferred(),
                ft.getCreatedAt(),
                ft.getAcceptedAt(),
                ft.getCompletedAt());
    }
}