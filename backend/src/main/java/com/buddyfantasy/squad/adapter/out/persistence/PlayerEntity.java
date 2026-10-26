package com.buddyfantasy.squad.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "squad_player")
class PlayerEntity {

    @Id
    UUID id;

    @Column(name = "league_id", nullable = false)
    UUID leagueId;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "shirt_number")
    Short shirtNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    Position position;

    @Column(name = "initial_value", nullable = false)
    BigDecimal initialValue;

    @Column(name = "photo_key")
    String photoKey;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "retired_at")
    Instant retiredAt;

    protected PlayerEntity() {
    }

    PlayerEntity(UUID id, UUID leagueId, String name, Short shirtNumber, Position position, BigDecimal initialValue,
                 String photoKey, Instant createdAt, Instant retiredAt) {
        this.id = id;
        this.leagueId = leagueId;
        this.name = name;
        this.shirtNumber = shirtNumber;
        this.position = position;
        this.initialValue = initialValue;
        this.photoKey = photoKey;
        this.createdAt = createdAt;
        this.retiredAt = retiredAt;
    }
}
