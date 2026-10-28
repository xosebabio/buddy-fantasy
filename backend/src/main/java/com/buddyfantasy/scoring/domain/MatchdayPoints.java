package com.buddyfantasy.scoring.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The managers' points in one scored match, as {@link Standings} reads them: those with a locked lineup, over
 * budget included (with 0). A member without a lineup is absent and scores 0.
 */
public record MatchdayPoints(UUID matchId, Instant kickoffAt, Map<UUID, Integer> managerPoints) {

    public MatchdayPoints {
        managerPoints = Map.copyOf(managerPoints);
    }

    public int pointsOf(UUID userId) {
        return managerPoints.getOrDefault(userId, 0);
    }

    /** The match counts for a member since {@code memberSince} if they already were one at kickoff. */
    public boolean countsFor(Instant memberSince) {
        return Standings.countsFor(kickoffAt, memberSince);
    }
}
