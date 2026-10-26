package com.buddyfantasy.league.adapter.out.persistence;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "league_scoring_rule")
@IdClass(ScoringRuleKey.class)
class ScoringRuleEntity {

    @Id
    @Column(name = "league_id")
    UUID leagueId;

    @Id
    @Enumerated(EnumType.STRING)
    ScoringAction action;

    @Id
    @Enumerated(EnumType.STRING)
    Position position;

    @Column(nullable = false)
    int points;

    protected ScoringRuleEntity() {
    }

    ScoringRuleEntity(UUID leagueId, ScoringAction action, Position position, int points) {
        this.leagueId = leagueId;
        this.action = action;
        this.position = position;
        this.points = points;
    }
}
