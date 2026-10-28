package com.buddyfantasy.scoring.application.usecases.getmatchdayscores;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.scoring.domain.PlayerScore;

/**
 * A player of the sheet with his points in the match: for actions plus for ratings. The position is
 * the one in the sheet.
 */
public record MatchdayPlayerView(UUID id, Position position, int actionPoints, int ratingPoints, int points) {

    static MatchdayPlayerView from(PlayerScore score) {
        return new MatchdayPlayerView(score.playerId(), score.position(), score.actionPoints(), score.ratingPoints(),
                score.points());
    }
}
