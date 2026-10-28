package com.buddyfantasy.scoring.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.league.ScoringTable;

/**
 * A scored match as seen by one manager, for their history (RF-33). {@code lineup} is their locked lineup, null if
 * they had none (0 points); {@code lines} are the sheet lines of its players who played, by player: one who did not
 * play is absent and scores 0. {@code rules} is the table the match was scored with.
 */
public record ManagerMatchday(UUID matchId, int matchday, Instant kickoffAt, ScoringTable rules,
                              ManagerLineup lineup, int points, Map<UUID, ScoredLine> lines) {

    public ManagerMatchday {
        lines = Map.copyOf(lines);
    }

    /** As in {@link Standings}: the match counts for a member since {@code memberSince} if they were one at kickoff. */
    public boolean countsFor(Instant memberSince) {
        return Standings.countsFor(kickoffAt, memberSince);
    }
}
