package com.buddyfantasy.lineup;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * A postponed match moved its lock back to the future, so its lineups are no longer locked; {@code scoring} drops
 * its copy. A new {@link LineupsLocked} follows when the new lock time comes.
 */
public record LineupsUnlocked(UUID leagueId, UUID matchId, Instant occurredAt) implements DomainEvent {
}
