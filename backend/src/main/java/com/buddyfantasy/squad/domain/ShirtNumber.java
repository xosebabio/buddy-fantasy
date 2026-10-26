package com.buddyfantasy.squad.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/** Optional on the player; uniqueness among active players of a league is enforced by the database. */
public record ShirtNumber(int value) {

    public static final int MIN = 0;
    public static final int MAX = 99;

    public ShirtNumber {
        if (value < MIN || value > MAX) {
            throw new DomainValidationException("Shirt number must be " + MIN + "-" + MAX);
        }
    }

    /** {@code null} means the player has no shirt number. */
    public static ShirtNumber ofNullable(Integer value) {
        return value == null ? null : new ShirtNumber(value);
    }
}
