package com.buddyfantasy.match;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Public API of the match module: the calendar for other modules, without results or sheets. */
public interface MatchApi {

    /**
     * Every match of the league in a single query, closed ones included, ordered by {@code kickoffAt} (the
     * chronological order, not the matchday number).
     */
    List<MatchSchedule> findMatches(UUID leagueId);

    /**
     * Scheduled (not closed) matches of every league whose kickoff is strictly before {@code until}, past ones
     * included, in a single query ordered by {@code kickoffAt}. Used by the lineup lock job.
     */
    List<MatchSchedule> findScheduledKickingOffBefore(Instant until);
}
