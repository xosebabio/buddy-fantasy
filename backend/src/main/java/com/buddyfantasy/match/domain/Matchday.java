package com.buddyfantasy.match.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Matchday number, unique per league (enforced by the database). A label: chronological order is the kickoff. Only
 * the storage range is checked here; the allowed minimum is {@link MatchLimits}.
 */
public record Matchday(int value) {

    /** Fits a non-negative {@code SMALLINT}. */
    public static final int MIN = 0;
    public static final int MAX = Short.MAX_VALUE;

    public Matchday {
        if (value < MIN || value > MAX) {
            throw new DomainValidationException("Matchday must be " + MIN + "-" + MAX);
        }
    }
}
