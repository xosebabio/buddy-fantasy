package com.buddyfantasy.match;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;

/**
 * One line of a match sheet as published in {@link MatchClosed} and {@link MatchCorrected}. {@code position} is the
 * snapshot taken when the player was added to the sheet.
 */
public record PlayerPerformanceData(
        UUID playerId,
        Position position,
        int goals,
        int penaltyGoals,
        int penaltiesSaved,
        int penaltiesMissed,
        int assists,
        int ownGoals,
        boolean yellowCard,
        boolean doubleYellow,
        boolean redCard) {
}
