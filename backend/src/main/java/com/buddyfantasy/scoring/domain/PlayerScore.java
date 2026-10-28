package com.buddyfantasy.scoring.domain;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.scoring.PlayerScoreData;

/**
 * A player's points in a match (RF-15, RF-30): points for actions plus points for ratings (RF-22, RF-23), without
 * weights.
 */
public record PlayerScore(UUID playerId, Position position, int actionPoints, int ratingPoints) {

    public int points() {
        return actionPoints + ratingPoints;
    }

    public PlayerScoreData toData() {
        return new PlayerScoreData(playerId, position, actionPoints, ratingPoints, points());
    }
}
