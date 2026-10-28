package com.buddyfantasy.scoring.application.usecases.getmanagerhistory;

import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.ScoringTable;
import com.buddyfantasy.scoring.domain.ScoredLine;

/**
 * A player of a locked lineup with their points in the match: per action (only those that add or subtract), for
 * ratings (0 until phase 2) and in total. One who did not play scores 0, with no position: the position is the one in
 * the sheet.
 */
public record LinedUpPlayerView(UUID id, boolean played, Position position, Map<ScoringAction, Integer> actions,
                                int actionPoints, int ratingPoints, int points) {

    /** {@code line} is null if the player did not play. */
    static LinedUpPlayerView from(UUID playerId, ScoredLine line, ScoringTable rules) {
        if (line == null) {
            return new LinedUpPlayerView(playerId, false, null, Map.of(), 0, 0, 0);
        }
        return new LinedUpPlayerView(playerId, true, line.performance().position(), line.breakdown(rules).points(),
                line.actionPoints(), line.ratingPoints(), line.points());
    }
}
