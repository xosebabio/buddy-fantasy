package com.buddyfantasy.lineup.application.port.out;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.lineup.domain.MatchLineupLock;

/** The materialized locks of matches (RF-25, RF-27): at most one per match, never updated, deleted to unlock. */
public interface MatchLineupLockRepository {

    /**
     * Inserts the lock with its lineups unless the match is already locked, also by a concurrent transaction. True
     * only for the call that locked it, which then publishes its events.
     */
    boolean saveIfAbsent(MatchLineupLock lock);

    /** The lock with its lineups and players, in a single query. */
    Optional<MatchLineupLock> findByMatchId(UUID matchId);

    /** Those of {@code matchIds} that are locked, in a single query. */
    Set<UUID> findLockedMatchIds(Collection<UUID> matchIds);

    /** Deletes the lock with its lineups; nothing if the match is not locked. */
    void deleteByMatchId(UUID matchId);
}
