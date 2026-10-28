package com.buddyfantasy.squad.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Optional on the player; uniqueness among active players of a league is enforced by the database. Only the storage
 * range is checked here; the allowed range is {@link SquadLimits}.
 */
public record ShirtNumber(int value) {

    /** Fits a non-negative {@code SMALLINT}. */
    public static final int MIN = 0;
    public static final int MAX = Short.MAX_VALUE;

    public ShirtNumber {
        if (value < MIN || value > MAX) {
            throw new DomainValidationException(SquadError.INVALID_SHIRT_NUMBER,
                    "Shirt number must be " + MIN + "-" + MAX);
        }
    }

    /** {@code null} means the player has no shirt number. */
    public static ShirtNumber ofNullable(Integer value) {
        return value == null ? null : new ShirtNumber(value);
    }
}
