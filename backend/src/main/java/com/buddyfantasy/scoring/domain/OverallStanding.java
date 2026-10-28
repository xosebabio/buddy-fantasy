package com.buddyfantasy.scoring.domain;

import java.util.UUID;

/**
 * A row of the overall standings (RF-32): the points of the scored matches since the member joined, how many they
 * are and the points of the last one (0 if none). Tied members share the position.
 */
public record OverallStanding(int position, UUID userId, int points, int matchdays, int lastMatchdayPoints) {

    OverallStanding atPosition(int newPosition) {
        return new OverallStanding(newPosition, userId, points, matchdays, lastMatchdayPoints);
    }
}
