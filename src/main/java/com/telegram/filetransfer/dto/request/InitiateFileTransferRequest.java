package com.telegram.filetransfer.dto.request;

public record InitiateFileTransferRequest(
        Long chatId,
        Long receiverId,
        String fileName,
        Long fileSize,
        String mimeType,
        String checksum
) {}