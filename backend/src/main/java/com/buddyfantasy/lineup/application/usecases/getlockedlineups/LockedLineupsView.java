package com.buddyfantasy.lineup.application.usecases.getlockedlineups;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.lineup.domain.MatchLineupLock;

/**
 * The lineups locked for a match, as frozen at {@code lockedAt}: members without a lineup saved before
 * {@code lockAt} are absent, as are those who were no longer members then. Ordered by user id.
 */
public record LockedLineupsView(UUID matchId, int matchday, Instant kickoffAt, Instant lockAt, Instant lockedAt,
                                List<LockedLineupView> lineups) {

    static LockedLineupsView from(MatchLineupLock lock) {
        return new LockedLineupsView(lock.matchId(), lock.matchday(), lock.kickoffAt(), lock.lockAt(),
                lock.lockedAt(), lock.lineups().stream().map(LockedLineupView::from).toList());
    }
}
