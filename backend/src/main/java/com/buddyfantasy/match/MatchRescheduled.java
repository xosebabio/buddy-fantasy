package com.buddyfantasy.match;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/**
 * The matchday or kickoff of a scheduled match changed (postponed). {@code lineup} recomputes the lineup lock from
 * the new kickoff, unlocking lineups if needed.
 */
public record MatchRescheduled(UUID leagueId, UUID matchId, int matchday, Instant previousKickoffAt,
        Instant kickoffAt, Instant occurredAt) implements DomainEvent {
}
