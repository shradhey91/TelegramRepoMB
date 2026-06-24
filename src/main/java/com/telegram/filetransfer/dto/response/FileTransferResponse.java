package com.telegram.filetransfer.dto.response;

import com.telegram.common.enums.FileTransferStatus;

import java.time.LocalDateTime;

public record FileTransferResponse(
        Long id,
        Long chatId,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        String fileName,
        Long fileSize,
        String mimeType,
        String checksum,
        FileTransferStatus status,
        Long bytesTransferred,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt
) {}