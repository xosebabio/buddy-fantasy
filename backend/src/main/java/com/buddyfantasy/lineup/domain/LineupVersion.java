package com.buddyfantasy.lineup.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * An immutable save of a manager's working lineup in a league. The lineup of a match is the last version saved
 * before its lock ({@link LineupTimeline}), so versions are never edited or deleted while the member plays.
 */
public record LineupVersion(UUID id, UUID leagueId, UUID userId, Instant savedAt, Set<UUID> playerIds) {

    public LineupVersion {
        playerIds = Set.copyOf(playerIds);
    }

    /** The players must have been validated with {@link LineupRules#requireValid} first. */
    public static LineupVersion save(UUID leagueId, UUID userId, Collection<UUID> playerIds, Instant now) {
        return new LineupVersion(UUID.randomUUID(), leagueId, userId, now, Set.copyOf(playerIds));
    }

    /**
     * Saved during the manager's current membership, which began at {@code memberSince}. A version saved before it
     * belongs to a past membership and no longer counts: who leaves the league and comes back starts from scratch.
     */
    public boolean savedSince(Instant memberSince) {
        return !savedAt.isBefore(memberSince);
    }

    /** Same players, in any order. */
    public boolean hasPlayers(Collection<UUID> otherPlayerIds) {
        return playerIds.equals(Set.copyOf(otherPlayerIds));
    }
}
