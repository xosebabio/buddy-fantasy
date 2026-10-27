package com.buddyfantasy.league.domain;

import java.time.Instant;

import com.buddyfantasy.shared.domain.AggregateRoot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class League extends AggregateRoot {

    private final LeagueId id;
    private final String name;
    private final Instant createdAt;
    private final LeagueSettings settings;
    private final ScoringRules scoringRules;

    /** Creates a league seeded with the configured default settings and scoring table (RF-05, RF-07). */
    public static League create(String name, Instant now, LeagueSettings settings, ScoringRules scoringRules,
                                LeagueLimits limits) {
        limits.requireValidName(name);
        limits.requireWithin(settings);
        return new League(LeagueId.generate(), name, now, settings, scoringRules);
    }

    /** Replaces the whole parameter set (RF-07); past scores are not recalculated. */
    public League withSettings(LeagueSettings newSettings, ScoringRules newScoringRules, LeagueLimits limits) {
        limits.requireWithin(newSettings);
        return new League(id, name, createdAt, newSettings, newScoringRules);
    }
}
