package com.telegram.call.entity;

import com.telegram.auth.entity.User;
import com.telegram.common.enums.CallStatus;
import com.telegram.common.enums.CallType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "calls", indexes = {
        @Index(name = "idx_calls_caller", columnList = "caller_id"),
        @Index(name = "idx_calls_receiver", columnList = "receiver_id"),
        @Index(name = "idx_calls_status", columnList = "status"),
        @Index(name = "idx_calls_created", columnList = "createdAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    @Column(nullable = false)
    @Builder.Default
    private Long durationSeconds = 0L;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void endCall() {
        this.endedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.status = CallStatus.ENDED;
        if (this.startedAt != null) {
            this.durationSeconds = Duration.between(this.startedAt, this.endedAt).getSeconds();
        }
    }
}