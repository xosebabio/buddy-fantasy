package com.buddyfantasy.league.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "league_invitation")
class InvitationEntity {

    @Id
    UUID id;

    @Column(name = "league_id", nullable = false)
    UUID leagueId;

    @Column(name = "token_hash", nullable = false)
    String tokenHash;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "revoked_at")
    Instant revokedAt;

    protected InvitationEntity() {
    }

    InvitationEntity(UUID id, UUID leagueId, String tokenHash, Instant createdAt, Instant revokedAt) {
        this.id = id;
        this.leagueId = leagueId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }
}
