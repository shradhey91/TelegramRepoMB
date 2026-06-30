package com.telegram.call.entity;

import com.telegram.auth.entity.User;
import com.telegram.common.enums.ParticipantRole;
import com.telegram.common.enums.ParticipantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "call_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"call_id", "user_id"})
}, indexes = {
        @Index(name = "idx_call_participants_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private Call call;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Boolean muted = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean cameraEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean screenSharing = false;

    private OffsetDateTime joinedAt;

    private OffsetDateTime leftAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}