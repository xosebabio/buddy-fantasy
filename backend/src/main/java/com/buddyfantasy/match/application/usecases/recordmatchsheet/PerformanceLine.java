package com.buddyfantasy.match.application.usecases.recordmatchsheet;

import java.util.UUID;

/** One player who played and their stats as sent by the admin; the position comes from {@code squad}. */
public record PerformanceLine(
        UUID playerId,
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
