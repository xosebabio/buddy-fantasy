package com.buddyfantasy.league.domain;

import java.time.Instant;

import com.buddyfantasy.shared.domain.AggregateRoot;
import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class League extends AggregateRoot {

    public static final int MAX_NAME_LENGTH = 100;

    private final LeagueId id;
    private final String name;
    private final Instant createdAt;
    private final LeagueSettings settings;
    private final ScoringRules scoringRules;

    /** Creates a league seeded with the default settings and scoring table (RF-05, RF-07). */
    public static League create(String name, Instant now) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new DomainValidationException("League name must be 1-" + MAX_NAME_LENGTH + " characters");
        }
        return new League(LeagueId.generate(), name, now, LeagueSettings.defaults(), ScoringRules.defaults());
    }

    /** Replaces the whole parameter set (RF-07); past scores are not recalculated. */
    public League withSettings(LeagueSettings newSettings, ScoringRules newScoringRules) {
        return new League(id, name, createdAt, newSettings, newScoringRules);
    }
}
