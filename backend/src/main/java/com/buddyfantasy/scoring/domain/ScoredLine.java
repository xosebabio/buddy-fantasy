package com.buddyfantasy.scoring.domain;

import com.buddyfantasy.league.ScoringTable;

/**
 * A line of the sheet of a scored match with its points (RF-11, RF-33). The breakdown per action is not stored: it is
 * the line times the table the match was scored with.
 */
public record ScoredLine(PlayerPerformance performance, int actionPoints, int ratingPoints) {

    public int points() {
        return actionPoints + ratingPoints;
    }

    /** The points per action with {@code rules}, the table frozen when the match was scored. */
    public ActionBreakdown breakdown(ScoringTable rules) {
        return ActionScoring.breakdown(performance, rules);
    }
}
