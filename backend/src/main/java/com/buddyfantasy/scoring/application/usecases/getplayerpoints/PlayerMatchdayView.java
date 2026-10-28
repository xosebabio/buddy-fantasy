package com.buddyfantasy.scoring.application.usecases.getplayerpoints;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.scoring.domain.PlayerMatchday;

/**
 * A player's points in a scored match: per action (only those that add or subtract), for ratings
 * and in total. The position is the one in the sheet.
 */
public record PlayerMatchdayView(UUID matchId, int matchday, Instant kickoffAt, Position position,
                                 Map<ScoringAction, Integer> actions, int actionPoints, int ratingPoints, int points) {

    static PlayerMatchdayView from(PlayerMatchday matchday) {
        return new PlayerMatchdayView(matchday.matchId(), matchday.matchday(), matchday.kickoffAt(),
                matchday.line().performance().position(), matchday.line().breakdown(matchday.rules()).points(),
                matchday.line().actionPoints(), matchday.line().ratingPoints(), matchday.line().points());
    }
}
