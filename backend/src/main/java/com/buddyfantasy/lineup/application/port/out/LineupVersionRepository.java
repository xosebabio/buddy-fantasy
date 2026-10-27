package com.buddyfantasy.lineup.application.port.out;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.lineup.domain.LineupVersion;

/** Versions are insert-only: the lineup of a match is the last one saved before its lock. */
public interface LineupVersionRepository {

    /** Every version of a manager in a league with its players, in a single query, ordered by {@code savedAt}. */
    List<LineupVersion> findByLeagueIdAndUserId(UUID leagueId, UUID userId);

    void save(LineupVersion version);
}
