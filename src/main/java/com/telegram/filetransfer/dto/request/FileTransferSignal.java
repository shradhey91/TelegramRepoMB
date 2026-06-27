package com.telegram.filetransfer.dto.request;

public record FileTransferSignal(
        Long transferId,
        Long receiverId,
        String type,
        String payload
) {}