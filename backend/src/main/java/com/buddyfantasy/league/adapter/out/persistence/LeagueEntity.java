package com.buddyfantasy.league.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "league_league")
class LeagueEntity {

    @Id
    UUID id;

    @Column(nullable = false)
    String name;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected LeagueEntity() {
    }

    LeagueEntity(UUID id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }
}
