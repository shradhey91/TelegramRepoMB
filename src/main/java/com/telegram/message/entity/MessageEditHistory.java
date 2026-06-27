package com.telegram.message.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "message_edit_history", indexes = {
        @Index(name = "idx_edit_history_message", columnList = "message_id, editedAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String oldContent;

    private OffsetDateTime editedAt;

    @PrePersist
    protected void onCreate() {
        editedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
