package com.buddyfantasy.league.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "league_membership")
class MembershipEntity {

    @Id
    UUID id;

    @Column(name = "league_id", nullable = false)
    UUID leagueId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    LeagueRole role;

    @Column(name = "joined_at", nullable = false)
    Instant joinedAt;

    protected MembershipEntity() {
    }

    MembershipEntity(UUID id, UUID leagueId, UUID userId, LeagueRole role, Instant joinedAt) {
        this.id = id;
        this.leagueId = leagueId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }
}
