package com.buddyfantasy.match.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Final score from the team's point of view. Only the storage range is checked here; the allowed maximum is
 * {@link MatchLimits}.
 */
public record MatchResult(int goalsFor, int goalsAgainst) {

    /** Fits {@code SMALLINT}. */
    public static final int MAX_STORED_GOALS = Short.MAX_VALUE;

    public MatchResult {
        if (goalsFor < 0 || goalsFor > MAX_STORED_GOALS || goalsAgainst < 0 || goalsAgainst > MAX_STORED_GOALS) {
            throw new DomainValidationException(MatchError.INVALID_GOALS, "Goals must be 0-" + MAX_STORED_GOALS);
        }
    }
}
