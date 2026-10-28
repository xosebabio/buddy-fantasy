package com.buddyfantasy.scoring.application.usecases.getmatchdayscores;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.scoring.domain.ManagerLineup;
import com.buddyfantasy.scoring.domain.MatchdayStanding;

/**
 * A row of the standings of a match: tied members share the position. {@code playerIds} is the locked lineup,
 * ordered by id, and empty for a member without one (0 points). Over budget scores 0 (RF-29).
 */
public record MatchdayStandingView(int position, UUID userId, int points, boolean overBudget, List<UUID> playerIds) {

    /** {@code lineup} is null if the member had no locked lineup. */
    static MatchdayStandingView from(MatchdayStanding standing, ManagerLineup lineup) {
        return new MatchdayStandingView(standing.position(), standing.userId(), standing.points(),
                lineup != null && lineup.overBudget(), lineup == null ? List.of() : lineup.playerIds());
    }
}
