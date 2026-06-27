package com.telegram.auth.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;


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

        private OffsetDateTime lastSeenAt;

        private OffsetDateTime createdAt;

        private OffsetDateTime updatedAt;

        @PrePersist
        protected void onCreate() {
                createdAt = OffsetDateTime.now(ZoneOffset.UTC);
                updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
}