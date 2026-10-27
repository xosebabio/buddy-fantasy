package com.buddyfantasy.lineup.application.usecases.findmatchestolock;

import java.time.Duration;
import java.util.List;

import com.buddyfantasy.shared.application.Query;

/**
 * For the lock job: scheduled matches of every league, not locked yet, whose lock time is at least {@code margin}
 * ago ({@code lockAt ≤ now − margin}), ordered by kickoff. The margin lets saves made just before a lock commit
 * before it is read.
 */
public record FindMatchesToLockQuery(Duration margin) implements Query<List<MatchToLock>> {
}
