package com.buddyfantasy.match.domain;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * A player who played the match and their stats (RF-13, RF-14). {@code playerId} is a logical reference to the
 * squad module and {@code position} the snapshot taken when they were added to the sheet. A yellow card and a
 * straight red add up; a double yellow is the only sending-off and already includes the yellow, so it excludes both.
 * Only the storage range of each count is checked here; the allowed maximum is {@link MatchLimits}.
 */
public record PlayerPerformance(
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

    /** Fits {@code SMALLINT}. */
    public static final int MAX_STORED_COUNT = Short.MAX_VALUE;

    public PlayerPerformance {
        if (playerId == null) {
            throw new DomainValidationException("Player is required");
        }
        if (position == null) {
            throw new DomainValidationException("Player position is required");
        }
        requireCount("goals", goals);
        requireCount("penalty goals", penaltyGoals);
        requireCount("penalties saved", penaltiesSaved);
        requireCount("penalties missed", penaltiesMissed);
        requireCount("assists", assists);
        requireCount("own goals", ownGoals);
        if (doubleYellow && (yellowCard || redCard)) {
            throw new DomainValidationException(MatchError.INVALID_CARDS,
                    "A double yellow excludes the yellow card and the straight red");
        }
    }

    public PlayerPerformanceData toData() {
        return new PlayerPerformanceData(playerId, position, goals, penaltyGoals, penaltiesSaved,
                penaltiesMissed, assists, ownGoals, yellowCard, doubleYellow, redCard);
    }

    private static void requireCount(String name, int value) {
        if (value < 0 || value > MAX_STORED_COUNT) {
            throw new DomainValidationException(MatchError.INVALID_STAT_COUNT,
                    "Number of " + name + " must be 0-" + MAX_STORED_COUNT);
        }
    }
}
