package com.telegram.storage.dto;

public record UploadResult(
        String fileId,
        String fileName,
        String url,
        String contentType,
        long size
) {
}
