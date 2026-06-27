package com.telegram.filetransfer.dto.response;

import com.telegram.common.enums.FileTransferStatus;

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
        java.time.OffsetDateTime createdAt,
        java.time.OffsetDateTime acceptedAt,
        java.time.OffsetDateTime completedAt
) {}