package com.telegram.message.controller;

import com.telegram.message.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long attachmentId) {

        AttachmentService.DownloadResponse response =
                attachmentService.downloadAttachment(attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + response.fileName() + "\""
                )
                .body(response.data());
    }
}