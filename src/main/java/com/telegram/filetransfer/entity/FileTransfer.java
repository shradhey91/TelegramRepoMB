package com.telegram.filetransfer.entity;

import com.telegram.auth.entity.User;
import com.telegram.chat.entity.Chat;
import com.telegram.common.enums.FileTransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "file_transfers", indexes = {
        @Index(name = "idx_ft_chat", columnList = "chat_id"),
        @Index(name = "idx_ft_sender", columnList = "sender_id"),
        @Index(name = "idx_ft_receiver", columnList = "receiver_id"),
        @Index(name = "idx_ft_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 500)
    private String fileName;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 100)
    private String mimeType;

    @Column(length = 64)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileTransferStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Long bytesTransferred = 0L;

    private OffsetDateTime acceptedAt;

    private OffsetDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (status == null) status = FileTransferStatus.PENDING;
        if (bytesTransferred == null) bytesTransferred = 0L;
    }
}