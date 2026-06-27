package com.telegram.chat.entity;

import com.telegram.auth.entity.User;
import com.telegram.message.entity.Message;
import com.telegram.common.enums.MemberRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "chat_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_id", "user_id"})
}, indexes = {
        @Index(name = "idx_chat_members_user", columnList = "user_id"),
        @Index(name = "idx_chat_members_chat", columnList = "chat_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private Message lastReadMessage;

    private OffsetDateTime joinedAt;

    private OffsetDateTime mutedUntil;

    @PrePersist
    protected void onCreate() {
        joinedAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (role == null) role = MemberRole.MEMBER;
    }
}
