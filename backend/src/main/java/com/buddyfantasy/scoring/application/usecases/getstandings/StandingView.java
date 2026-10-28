package com.buddyfantasy.scoring.application.usecases.getstandings;

import java.util.UUID;

import com.buddyfantasy.scoring.domain.OverallStanding;

/**
 * A row of the overall standings: tied members share the position (1, 1, 3). {@code matchdays} are the scored
 * matches counted for the member and {@code lastMatchdayPoints} the points of the last one (0 if none).
 */
public record StandingView(int position, UUID userId, int points, int matchdays, int lastMatchdayPoints) {

    static StandingView from(OverallStanding standing) {
        return new StandingView(standing.position(), standing.userId(), standing.points(), standing.matchdays(),
                standing.lastMatchdayPoints());
    }
}
