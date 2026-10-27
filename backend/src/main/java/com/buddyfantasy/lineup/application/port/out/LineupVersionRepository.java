package com.buddyfantasy.lineup.application.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.lineup.domain.LineupVersion;

/** Versions are insert-only: the lineup of a match is the last one saved before its lock. */
public interface LineupVersionRepository {

    /** Every version of a manager in a league with its players, in a single query, ordered by {@code savedAt}. */
    List<LineupVersion> findByLeagueIdAndUserId(UUID leagueId, UUID userId);

    /**
     * For each of {@code userIds}, the last version saved strictly before {@code before} with its players, as
     * {@link com.buddyfantasy.lineup.domain.LineupTimeline#lineupFor} picks it, in a single query. Users without one
     * are absent.
     */
    List<LineupVersion> findLastSavedBefore(UUID leagueId, Collection<UUID> userIds, Instant before);

    void save(LineupVersion version);
}
