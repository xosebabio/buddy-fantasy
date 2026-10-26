package com.buddyfantasy.identity.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_user")
class UserEntity {

    @Id
    UUID id;

    @Column(name = "google_sub", nullable = false)
    String googleSub;

    @Column(nullable = false)
    String email;

    @Column(name = "display_name", nullable = false)
    String displayName;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "last_login_at", nullable = false)
    Instant lastLoginAt;

    protected UserEntity() {
    }

    UserEntity(UUID id, String googleSub, String email, String displayName, String avatarUrl,
               Instant createdAt, Instant lastLoginAt) {
        this.id = id;
        this.googleSub = googleSub;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }
}
