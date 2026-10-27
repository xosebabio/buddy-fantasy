package com.buddyfantasy.match.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import jakarta.validation.constraints.NotNull;

/** Missing counters are 0 and missing cards false. The position is not sent: it comes from the squad. */
record PlayerPerformanceRequest(@NotNull UUID playerId,
                                int goals,
                                int penaltyGoals,
                                int penaltiesSaved,
                                int penaltiesMissed,
                                int assists,
                                int ownGoals,
                                boolean yellowCard,
                                boolean doubleYellow,
                                boolean redCard) {

    PerformanceLine toLine() {
        return new PerformanceLine(playerId, goals, penaltyGoals, penaltiesSaved, penaltiesMissed, assists, ownGoals,
                yellowCard, doubleYellow, redCard);
    }
}
