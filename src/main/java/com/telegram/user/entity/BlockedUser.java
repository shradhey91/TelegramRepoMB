package com.telegram.user.entity;

import com.telegram.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "blocked_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
}, indexes = {
        @Index(name = "idx_blocked_users_blocker", columnList = "blocker_id"),
        @Index(name = "idx_blocked_users_blocked", columnList = "blocked_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    private OffsetDateTime blockedAt;

    @PrePersist
    protected void onCreate() {
        blockedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}