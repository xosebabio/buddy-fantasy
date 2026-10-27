package com.buddyfantasy.match;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/** A scheduled match was deleted (suspended and not replayed). Closed matches are never deleted. */
public record MatchDeleted(UUID leagueId, UUID matchId, int matchday, Instant occurredAt) implements DomainEvent {
}
