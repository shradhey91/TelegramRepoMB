package com.telegram.entities;

import com.telegram.enums.CallStatus;
import com.telegram.enums.CallType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

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

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Column(nullable = false)
    @Builder.Default
    private Long durationSeconds = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void endCall() {
        this.endedAt = LocalDateTime.now();
        this.status = CallStatus.ENDED;
        if (this.startedAt != null) {
            this.durationSeconds = Duration.between(this.startedAt, this.endedAt).getSeconds();
        }
    }
}