package com.buddyfantasy.scoring.domain;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * scoring's copy of one line of a match sheet (RF-13, RF-14). {@code position} is the snapshot of the sheet, not the
 * player's current one. The sheet already splits goals from penalty goals and leaves out the yellow card of a double
 * yellow, so each stat maps to one action ({@link ActionScoring}).
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
    }

    private static void requireCount(String name, int value) {
        if (value < 0) {
            throw new DomainValidationException("Number of " + name + " cannot be negative");
        }
    }
}
