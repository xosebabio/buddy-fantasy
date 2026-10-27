package com.buddyfantasy.match.adapter.out.persistence;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** A row of {@code match_player_performance}; its key is {@code (match_id, player_id)}. */
@Embeddable
class PlayerPerformanceEmbeddable {

    @Column(name = "player_id", nullable = false)
    UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    Position position;

    @Column(name = "goals", nullable = false)
    short goals;

    @Column(name = "penalty_goals", nullable = false)
    short penaltyGoals;

    @Column(name = "penalties_saved", nullable = false)
    short penaltiesSaved;

    @Column(name = "penalties_missed", nullable = false)
    short penaltiesMissed;

    @Column(name = "assists", nullable = false)
    short assists;

    @Column(name = "own_goals", nullable = false)
    short ownGoals;

    @Column(name = "yellow_card", nullable = false)
    boolean yellowCard;

    @Column(name = "double_yellow", nullable = false)
    boolean doubleYellow;

    @Column(name = "red_card", nullable = false)
    boolean redCard;

    protected PlayerPerformanceEmbeddable() {
    }

    PlayerPerformanceEmbeddable(UUID playerId, Position position, short goals, short penaltyGoals,
                                short penaltiesSaved, short penaltiesMissed, short assists, short ownGoals,
                                boolean yellowCard, boolean doubleYellow, boolean redCard) {
        this.playerId = playerId;
        this.position = position;
        this.goals = goals;
        this.penaltyGoals = penaltyGoals;
        this.penaltiesSaved = penaltiesSaved;
        this.penaltiesMissed = penaltiesMissed;
        this.assists = assists;
        this.ownGoals = ownGoals;
        this.yellowCard = yellowCard;
        this.doubleYellow = doubleYellow;
        this.redCard = redCard;
    }
}
