package com.buddyfantasy.squad;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/** A player left the active squad; they stay in past matches, points and lineups. */
public record PlayerRetired(UUID leagueId, UUID playerId, Instant occurredAt) implements DomainEvent {
}
