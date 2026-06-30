package com.telegram.message.dto.response;

public record AttachmentResponse(
        Long id,
        String fileUrl,
        String fileName,
        Long fileSize,
        String mimeType,
        String thumbnailUrl
) {}