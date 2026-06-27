package com.telegram.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;


@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true, length = 50)
        private String username;

        @Column(nullable = false, unique = true)
        private String email;

        @Column(nullable = false)
        private String passwordHash;

        @Column(length = 100)
        private String displayName;

        @Column(length = 200)
        private String bio;

        @Column(length = 1024)
        private String avatarUrl;

        @Column(nullable = false)
        private Boolean isOnline;

        private LocalDateTime lastSeenAt;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        @PrePersist
        protected void onCreate() {
                createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
                updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
                if (isOnline == null) isOnline = false;
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        }
}