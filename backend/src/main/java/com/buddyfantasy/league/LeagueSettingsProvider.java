package com.buddyfantasy.league;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Public API of the league module: read-only access to a league's parameters for other modules. */
public interface LeagueSettingsProvider {

    /** Throws {@link com.buddyfantasy.shared.domain.NotFoundException} (404) if the league does not exist. */
    LineupSettings lineupSettings(UUID leagueId);

    /** One query for all ids. Unknown leagues are absent from the result. */
    Map<UUID, LineupSettings> lineupSettings(Collection<UUID> leagueIds);

    /**
     * The league's current table of points per action and position, in one query. Throws
     * {@link com.buddyfantasy.shared.domain.NotFoundException} (404) if the league does not exist.
     */
    ScoringTable scoringRules(UUID leagueId);

    /**
     * The league's current rating parameters, with the conversion of its current format, in one query. Throws
     * {@link com.buddyfantasy.shared.domain.NotFoundException} (404) if the league does not exist.
     */
    RatingSettings ratingSettings(UUID leagueId);

    /**
     * The configured maximum of {@link LineupSettings#lockHours()} ({@code league.limits.lineup-lock-hours-max}),
     * checked when a league saves its parameters: how far ahead of kickoff a lineup lock can be. Lowering it does not
     * change leagues that already have more.
     */
    int lineupLockHoursMax();
}
