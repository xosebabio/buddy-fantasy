package com.buddyfantasy.match.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/** Matchday number, unique per league (enforced by the database). A label: chronological order is the kickoff. */
public record Matchday(int value) {

    public static final int MIN = 1;
    /** Fits {@code SMALLINT}. */
    public static final int MAX = Short.MAX_VALUE;

    public Matchday {
        if (value < MIN || value > MAX) {
            throw new DomainValidationException("Matchday must be " + MIN + "-" + MAX);
        }
    }
}
