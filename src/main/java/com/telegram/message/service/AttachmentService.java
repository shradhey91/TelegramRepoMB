package com.telegram.message.service;

import com.telegram.common.exception.ResourceNotFoundException;
import com.telegram.message.entity.Attachment;
import com.telegram.message.repository.AttachmentRepo;
import com.telegram.storage.dto.StorageServiceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepo attachmentRepo;
    private final StorageServiceProvider storageServiceProvider;

    public DownloadResponse downloadAttachment(Long attachmentId) {

        Attachment attachment = attachmentRepo.findById(attachmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Attachment not found"));

        byte[] data = storageServiceProvider.download(
                attachment.getFileUrl());

        return new DownloadResponse(
                data,
                attachment.getMimeType(),
                attachment.getFileName()
        );
    }

    public record DownloadResponse(byte[] data, String contentType, String fileName) {
    }
}