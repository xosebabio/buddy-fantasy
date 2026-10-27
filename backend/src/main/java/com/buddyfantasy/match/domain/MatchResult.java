package com.buddyfantasy.match.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/** Final score from the team's point of view. */
public record MatchResult(int goalsFor, int goalsAgainst) {

    public static final int MAX_GOALS = 99;

    public MatchResult {
        if (goalsFor < 0 || goalsFor > MAX_GOALS || goalsAgainst < 0 || goalsAgainst > MAX_GOALS) {
            throw new DomainValidationException("Goals must be 0-" + MAX_GOALS);
        }
    }
}
