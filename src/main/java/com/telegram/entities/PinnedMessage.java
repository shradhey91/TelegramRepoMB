package com.telegram.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pinned_messages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_id", "message_id"})
}, indexes = {
        @Index(name = "idx_pinned_messages_chat", columnList = "chat_id, pinnedAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by", nullable = false)
    private User pinnedBy;

    private LocalDateTime pinnedAt;

    @PrePersist
    protected void onCreate() {
        pinnedAt = LocalDateTime.now();
    }
}