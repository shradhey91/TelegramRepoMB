package com.telegram.message.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "attachments", indexes = {
        @Index(name = "idx_attachments_message", columnList = "message_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(nullable = false)
    private String fileUrl;

    @Column(length = 255)
    private String fileName;

    private Long fileSize;

    @Column(length = 100)
    private String mimeType;

    private Integer durationSeconds;

    @Column(length = 1024)
    private String thumbnailUrl;

    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}